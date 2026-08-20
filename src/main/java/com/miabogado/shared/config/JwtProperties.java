package com.miabogado.shared.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank @Size(min = 32, message = "El secreto HS256 debe tener al menos 32 caracteres") String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
}
