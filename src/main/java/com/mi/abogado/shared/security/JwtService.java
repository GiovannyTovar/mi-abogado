package com.mi.abogado.shared.security;

import com.mi.abogado.domain.user.entity.Role;
import com.mi.abogado.domain.user.entity.User;
import com.mi.abogado.shared.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Emision y verificacion del JWT propio de la plataforma.
 * <p>
 * El ID token de Firebase solo se usa una vez, en el login, para probar quien es
 * la persona. A partir de ahi la app viaja con este token, que ademas lleva
 * {@code tenantId} y {@code role}: datos nuestros, no de Firebase.
 */
@Service
public class JwtService {

    private static final String CLAIM_TENANT = "tenantId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL = "email";

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtService(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.issuer();
        this.accessTokenTtl = properties.accessTokenTtl();
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_TENANT, user.tenantId().map(UUID::toString).orElse(null))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }

    /**
     * @return el principal si el token es valido; vacio si esta expirado,
     *         manipulado o mal formado (el filtro no distingue: en todos los casos es 401).
     */
    public Optional<AuthPrincipal> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tenantId = claims.get(CLAIM_TENANT, String.class);
            return Optional.of(new AuthPrincipal(
                    UUID.fromString(claims.getSubject()),
                    tenantId == null ? null : UUID.fromString(tenantId),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    claims.get(CLAIM_EMAIL, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
