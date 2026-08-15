package com.mi.abogado.domain.subscription.entity;

public enum SubscriptionStatus {
    /** En periodo de prueba: acceso completo hasta {@code trialEndsAt}. */
    TRIALING,
    ACTIVE,
    /** Pago vencido: la firma queda suspendida hasta regularizar. */
    PAST_DUE,
    CANCELLED
}
