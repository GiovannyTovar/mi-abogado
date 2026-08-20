package com.miabogado.domain.tenant.entity;

public enum TenantStatus {
    /** Periodo de prueba: acceso completo con fecha de corte (Fase 2). */
    TRIAL,
    ACTIVE,
    /** Impago o incumplimiento: solo lectura hasta regularizar. */
    SUSPENDED,
    CANCELLED
}
