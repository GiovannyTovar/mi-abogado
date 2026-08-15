package com.mi.abogado.domain.user.dto;

import com.mi.abogado.domain.user.entity.Role;
import com.mi.abogado.domain.user.entity.UserStatus;

import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID tenantId,
        String email,
        String fullName,
        String photoUrl,
        String phone,
        Role role,
        UserStatus status
) {
}
