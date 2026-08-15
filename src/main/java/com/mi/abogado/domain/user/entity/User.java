package com.mi.abogado.domain.user.entity;

import com.mi.abogado.domain.tenant.entity.Tenant;
import com.mi.abogado.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Identidad de la plataforma, espejo local del usuario de Firebase.
 * <p>
 * <b>No</b> hereda de {@code TenantScopedEntity} a proposito: el login busca por
 * {@code firebaseUid} antes de saber a que firma pertenece la persona, asi que el
 * filtro automatico de tenant haria imposible autenticar. El aislamiento de esta
 * tabla se hace explicito en los repositorios ({@code ...AndTenantId(...)}).
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    /** Null solo para SUPER_ADMIN (restriccion ck_app_user_tenant). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", updatable = false)
    private Tenant tenant;

    /** Null mientras el usuario esta invitado; se vincula en su primer login. */
    @Column(name = "firebase_uid", length = 128)
    private String firebaseUid;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public User(Tenant tenant, String email, String fullName, Role role) {
        this.tenant = tenant;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    /**
     * Lee el id sin disparar la carga del proxy de Tenant.
     */
    public Optional<UUID> tenantId() {
        return Optional.ofNullable(tenant).map(Tenant::getId);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * Primer login: vincula la cuenta de Google y activa al usuario invitado.
     */
    public void linkFirebaseAccount(String firebaseUid, String photoUrl) {
        this.firebaseUid = firebaseUid;
        this.photoUrl = photoUrl;
        this.status = UserStatus.ACTIVE;
    }

    public void registerLogin(Instant when) {
        this.lastLoginAt = when;
    }

    public void disable() {
        this.status = UserStatus.DISABLED;
    }

    /**
     * Reactivar a quien nunca llego a entrar lo devuelve a PENDING, no a ACTIVE:
     * ACTIVE significa "tiene cuenta de Google vinculada".
     */
    public void enable() {
        this.status = firebaseUid == null ? UserStatus.PENDING : UserStatus.ACTIVE;
    }
}
