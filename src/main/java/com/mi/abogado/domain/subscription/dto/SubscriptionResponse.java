package com.mi.abogado.domain.subscription.dto;

import com.mi.abogado.domain.subscription.entity.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * @param membersInUse miembros activos ahora mismo; el panel lo pinta contra
 *                     {@code plan.maxMembers} para avisar antes de que se llene.
 */
public record SubscriptionResponse(
        UUID id,
        SubscriptionStatus status,
        SubscriptionPlanResponse plan,
        Instant startedAt,
        Instant trialEndsAt,
        Instant currentPeriodEnd,
        long membersInUse
) {
}
