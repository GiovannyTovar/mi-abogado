package com.mi.abogado.domain.auth.service;

import com.mi.abogado.domain.auth.dto.AuthResponse;
import com.mi.abogado.domain.tenant.entity.Tenant;
import com.mi.abogado.domain.user.entity.User;
import com.mi.abogado.domain.user.entity.UserStatus;
import com.mi.abogado.domain.user.mapper.UserMapper;
import com.mi.abogado.domain.user.repository.UserRepository;
import com.mi.abogado.shared.error.BusinessException;
import com.mi.abogado.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Puente Firebase -> sesion propia.
 * <p>
 * Regla de negocio central: <b>no hay auto-registro</b>. Entrar con Google prueba
 * quien eres, no que tengas acceso. El usuario debe existir ya en {@code app_user}
 * porque alguien lo invito (el super-admin creo la firma y su dueno; el dueno invito
 * a sus abogados; la firma dio de alta a su cliente). Asi ningun usuario queda
 * huerfano, sin tenant ni rol.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final FirebaseTokenVerifier tokenVerifier;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse loginWithGoogle(String idToken, String userAgent) {
        GoogleIdentity identity = tokenVerifier.verify(idToken);
        if (!identity.emailVerified()) {
            throw BusinessException.unauthorized("La cuenta de Google no tiene el correo verificado");
        }

        User user = findLinkedUser(identity).orElseGet(() -> linkInvitedUser(identity));
        ensureCanLogIn(user);

        user.registerLogin(Instant.now());
        return buildSession(user, userAgent);
    }

    /**
     * Renueva la sesion sin volver a pasar por Google.
     */
    @Transactional
    public AuthResponse refresh(String refreshToken, String userAgent) {
        User user = refreshTokenService.consume(refreshToken);
        ensureCanLogIn(user);
        return buildSession(user, userAgent);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private Optional<User> findLinkedUser(GoogleIdentity identity) {
        return userRepository.findByFirebaseUid(identity.firebaseUid());
    }

    /** Primer login de un usuario invitado: se vincula su cuenta de Google a la fila existente. */
    private User linkInvitedUser(GoogleIdentity identity) {
        User invited = userRepository.findPendingInvitationByEmail(identity.email())
                .orElseThrow(() -> BusinessException.forbidden(
                        "Esta cuenta no tiene acceso. Pide a tu firma que te invite."));

        invited.linkFirebaseAccount(identity.firebaseUid(), identity.photoUrl());
        return invited;
    }

    private void ensureCanLogIn(User user) {
        if (user.getStatus() == UserStatus.DISABLED) {
            throw BusinessException.forbidden("Usuario desactivado");
        }
        // El super-admin no tiene firma; el resto no entra si su firma esta suspendida o cancelada.
        Tenant tenant = user.getTenant();
        if (tenant != null && !tenant.isOperational()) {
            throw BusinessException.forbidden("La suscripcion de la firma no esta activa");
        }
    }

    private AuthResponse buildSession(User user, String userAgent) {
        return new AuthResponse(
                jwtService.issueAccessToken(user),
                refreshTokenService.issue(user, userAgent),
                jwtService.accessTokenTtl().toSeconds(),
                userMapper.toResponse(user));
    }
}
