package com.miabogado.domain.user.repository;

import com.miabogado.domain.user.dto.MemberSummary;
import com.miabogado.domain.user.entity.Role;
import com.miabogado.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Login: trae el tenant en el mismo select para armar el JWT sin segunda consulta. */
    @EntityGraph(attributePaths = "tenant")
    Optional<User> findByFirebaseUid(String firebaseUid);

    /** Vinculacion del invitado en su primer login (email normalizado a minusculas). */
    @EntityGraph(attributePaths = "tenant")
    @Query("select u from User u where lower(u.email) = lower(:email) and u.firebaseUid is null")
    Optional<User> findPendingInvitationByEmail(@Param("email") String email);

    @Query("select u from User u where lower(u.email) = lower(:email) and u.role = :role")
    Optional<User> findByEmailAndRole(@Param("email") String email, @Param("role") Role role);

    /**
     * Lectura dentro de la firma. User no lleva {@code @TenantId}, asi que el filtro
     * por tenant va explicito aqui.
     */
    Optional<User> findByIdAndTenant_Id(UUID id, UUID tenantId);

    /**
     * Miembros del equipo que cuentan para el limite del plan: dueno, abogados y
     * asistentes. Los clientes finales no son plantilla de la firma, son sus
     * clientes; cobrar por ellos penalizaria a quien mas usa la herramienta.
     */
    @Query("""
            select count(u) from User u
            where u.tenant.id = :tenantId
              and u.status <> com.miabogado.domain.user.entity.UserStatus.DISABLED
              and u.role <> com.miabogado.domain.user.entity.Role.CLIENT
            """)
    long countActiveMembers(@Param("tenantId") UUID tenantId);

    boolean existsByTenant_IdAndEmailIgnoreCase(UUID tenantId, String email);

    /**
     * Equipo de la firma. Se excluyen los clientes finales: son otra pantalla
     * (el CRM de la Fase 3), no miembros del equipo.
     */
    @Query(value = """
            select new com.miabogado.domain.user.dto.MemberSummary(
                u.id, u.fullName, u.email, u.photoUrl, u.role, u.status, u.lastLoginAt)
            from User u
            where u.tenant.id = :tenantId
              and u.role <> com.miabogado.domain.user.entity.Role.CLIENT
              and (:role is null or u.role = :role)
            """,
            countQuery = """
            select count(u) from User u
            where u.tenant.id = :tenantId
              and u.role <> com.miabogado.domain.user.entity.Role.CLIENT
              and (:role is null or u.role = :role)
            """)
    Page<MemberSummary> findMembers(@Param("tenantId") UUID tenantId,
                                    @Param("role") Role role,
                                    Pageable pageable);
}
