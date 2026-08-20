package com.miabogado.domain.subscription.entity;

import com.miabogado.domain.tenant.entity.Tenant;
import com.miabogado.shared.persistence.BaseEntity;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Suscripcion vigente de una firma a la plataforma.
 * <p>
 * No hereda de {@code TenantScopedEntity}: es una tabla del plano de plataforma,
 * que el super-admin consulta entre firmas. El aislamiento va explicito en el
 * repositorio.
 */
@Entity
@Table(name = "subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    /** Fin del ciclo facturado. Lo movera la pasarela de pago cuando se integre. */
    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /**
     * Alta: si el plan trae dias de prueba arranca en TRIALING; si es gratuito
     * o no tiene prueba, ACTIVE desde el primer dia.
     */
    public Subscription(Tenant tenant, SubscriptionPlan plan) {
        this.tenant = tenant;
        this.plan = plan;

        if (plan.hasTrial()) {
            this.status = SubscriptionStatus.TRIALING;
            this.trialEndsAt = startedAt.plus(plan.getTrialDays(), ChronoUnit.DAYS);
            this.currentPeriodEnd = trialEndsAt;
        } else {
            this.status = SubscriptionStatus.ACTIVE;
            this.currentPeriodEnd = startedAt.plus(30, ChronoUnit.DAYS);
        }
    }

    public boolean isCurrent() {
        return status != SubscriptionStatus.CANCELLED;
    }

    /**
     * Cambio de plan sobre la misma suscripcion: no se reinicia la prueba
     * (si ya la gasto, cambiar de plan no le da otra).
     */
    public void changePlan(SubscriptionPlan newPlan) {
        this.plan = newPlan;
        if (status == SubscriptionStatus.TRIALING && !newPlan.hasTrial()) {
            this.status = SubscriptionStatus.ACTIVE;
            this.trialEndsAt = null;
        }
    }

    public void markPastDue() {
        this.status = SubscriptionStatus.PAST_DUE;
    }

    public void activate(Instant periodEnd) {
        this.status = SubscriptionStatus.ACTIVE;
        this.trialEndsAt = null;
        this.currentPeriodEnd = periodEnd;
    }

    public void cancel(Instant when) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = when;
    }
}
