package com.miabogado.domain.user.dto;

import com.miabogado.domain.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Invitacion de un asistente. Los abogados entran por {@code POST /api/v1/lawyers}
 * porque ademas del usuario necesitan perfil profesional (tarjeta, especialidades).
 *
 * @param role se admite por si mañana hay mas roles de equipo; hoy el service
 *             solo acepta ASSISTANT.
 */
public record InviteMemberRequest(
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 30) String phone,
        @NotNull Role role
) {
}
