package com.mi.abogado.domain.auth.service;

import com.mi.abogado.domain.auth.entity.RefreshToken;
import com.mi.abogado.domain.auth.repository.RefreshTokenRepository;
import com.mi.abogado.domain.user.entity.User;
import com.mi.abogado.shared.config.JwtProperties;
import com.mi.abogado.shared.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Emision, rotacion y revocacion de refresh tokens.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /**
     * @return el token en claro: es la unica vez que existe fuera del cliente.
     */
    @Transactional
    public String issue(User user, String userAgent) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshTokenRepository.save(new RefreshToken(
                user,
                hash(token),
                Instant.now().plus(jwtProperties.refreshTokenTtl()),
                userAgent));
        return token;
    }

    /**
     * Rotacion: el token usado se revoca en el acto. Si alguien reutiliza uno ya
     * consumido, no obtiene sesion.
     */
    @Transactional
    public User consume(String token) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> BusinessException.unauthorized("Refresh token invalido"));

        Instant now = Instant.now();
        if (!stored.isUsable(now)) {
            throw BusinessException.unauthorized("Refresh token expirado o revocado");
        }

        stored.revoke(now);
        return stored.getUser();
    }

    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByTokenHash(hash(token))
                .ifPresent(stored -> stored.revoke(Instant.now()));
    }

    @Transactional
    public void revokeAllSessions(User user) {
        refreshTokenRepository.revokeAllByUser(user.getId(), Instant.now());
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}
