package com.miabogado.domain.subscription.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param maxMembers     null = ilimitado
 * @param maxActiveCases null = ilimitado
 */
public record SubscriptionPlanResponse(
        UUID id,
        String code,
        String name,
        String description,
        BigDecimal monthlyPrice,
        String currency,
        short trialDays,
        Integer maxMembers,
        Integer maxActiveCases,
        boolean marketplaceEnabled,
        boolean whiteLabelEnabled
) {
}
