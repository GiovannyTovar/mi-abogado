package com.miabogado.domain.user.service;

import com.miabogado.domain.auth.service.RefreshTokenService;
import com.miabogado.domain.subscription.service.SubscriptionService;
import com.miabogado.domain.tenant.entity.Tenant;
import com.miabogado.domain.tenant.repository.TenantRepository;
import com.miabogado.domain.user.dto.InviteMemberRequest;
import com.miabogado.domain.user.dto.MemberSummary;
import com.miabogado.domain.user.dto.UserResponse;
import com.miabogado.domain.user.entity.Role;
import com.miabogado.domain.user.entity.User;
import com.miabogado.domain.user.entity.UserStatus;
import com.miabogado.domain.user.mapper.UserMapper;
import com.miabogado.domain.user.repository.UserRepository;
import com.miabogado.shared.error.BusinessException;
import com.miabogado.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Equipo de la firma: invitar, listar, activar y desactivar miembros.
 * <p>
 * No existe una entidad {@code Assistant}: un asistente es un {@code User} con rol
 * ASSISTANT y nada mas. Una tabla sin columnas propias solo anadiria un JOIN.
 * El abogado si tiene entidad propia porque tiene datos propios (tarjeta
 * profesional, especialidades, ficha publica).
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionService subscriptionService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public Page<MemberSummary> findMembers(UUID tenantId, Role role, Pageable pageable) {
        return userRepository.findMembers(tenantId, role, pageable);
    }

    /**
     * Invita a un asistente. Queda PENDING hasta que entre con Google.
     */
    @Transactional
    public UserResponse invite(UUID tenantId, InviteMemberRequest request) {
        if (request.role() != Role.ASSISTANT) {
            throw BusinessException.conflict(
                    "Solo se invitan asistentes por aqui. Los abogados se dan de alta en /api/v1/lawyers.");
        }
        if (userRepository.existsByTenant_IdAndEmailIgnoreCase(tenantId, request.email())) {
            throw BusinessException.conflict("Ya hay un usuario con ese correo en la firma");
        }
        subscriptionService.ensureCanAddMember(tenantId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("Firma"));

        User member = new User(tenant, request.email(), request.fullName(), request.role());
        member.setPhone(request.phone());
        return userMapper.toResponse(userRepository.save(member));
    }

    /**
     * Desactivar corta el acceso de inmediato: ademas de marcar DISABLED hay que
     * revocar los refresh tokens, o el usuario seguiria renovando su sesion.
     * (El access token vigente caduca solo en menos de 30 minutos.)
     */
    @Transactional
    public UserResponse changeStatus(UUID tenantId, UUID memberId, UserStatus status) {
        if (status == UserStatus.PENDING) {
            throw BusinessException.conflict("PENDING no se asigna a mano: es el estado de una invitacion sin usar");
        }
        if (memberId.equals(CurrentUser.require().userId())) {
            throw BusinessException.conflict("No puedes cambiar tu propio estado");
        }

        User member = userRepository.findByIdAndTenant_Id(memberId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("Miembro"));

        if (member.getRole() == Role.FIRM_OWNER) {
            throw BusinessException.forbidden("El dueno de la firma no se puede desactivar");
        }

        if (status == UserStatus.DISABLED) {
            member.disable();
            refreshTokenService.revokeAllSessions(member);
        } else {
            // Reactivar ocupa una plaza del plan: hay que comprobar el aforo otra vez.
            subscriptionService.ensureCanAddMember(tenantId);
            member.enable();
        }
        return userMapper.toResponse(member);
    }
}
