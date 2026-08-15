package com.mi.abogado.domain.subscription.entity;

import com.mi.abogado.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Plan comercial de la plataforma. Catalogo global: lo mantiene el super-admin.
 * Los limites nulos significan "sin limite".
 */
@Entity
@Table(name = "subscription_plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionPlan extends BaseEntity {

    @Column(nullable = false, length = 40, updatable = false)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 400)
    private String description;

    @Column(name = "monthly_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyPrice = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "COP";

    @Column(name = "trial_days", nullable = false)
    private short trialDays;

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "max_active_cases")
    private Integer maxActiveCases;

    @Column(name = "marketplace_enabled", nullable = false)
    private boolean marketplaceEnabled;

    @Column(name = "white_label_enabled", nullable = false)
    private boolean whiteLabelEnabled;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    public boolean hasTrial() {
        return trialDays > 0;
    }

    /**
     * @param currentMembers miembros activos de la firma ahora mismo
     */
    public boolean allowsOneMoreMember(long currentMembers) {
        return maxMembers == null || currentMembers < maxMembers;
    }
}
