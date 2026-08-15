package com.mi.abogado.domain.auth.dto;

import com.mi.abogado.domain.user.dto.UserResponse;

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
