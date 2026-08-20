package com.miabogado.domain.auth.dto;

import com.miabogado.domain.user.dto.UserResponse;

/**
 * @param expiresInSeconds vida del access token; el cliente refresca antes de que venza.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserResponse user
) {
}
