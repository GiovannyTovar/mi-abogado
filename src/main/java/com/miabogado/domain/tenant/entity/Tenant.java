package com.miabogado.domain.tenant.entity;

import com.miabogado.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * La firma / bufete. Raiz del aislamiento: no hereda de TenantScopedEntity
 * porque ella misma <i>es</i> el tenant.
 */
@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 80, updatable = false)
    private String slug;

    @Column(length = 30)
    private String nit;

    @Column(name = "contact_email", length = 180)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status = TenantStatus.TRIAL;

    public Tenant(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public boolean isOperational() {
        return status == TenantStatus.ACTIVE || status == TenantStatus.TRIAL;
    }
}
