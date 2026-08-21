package com.miabogado.domain.settlement.dto;

import java.math.BigDecimal;

/**
 * Un renglon del resultado. Lleva los dias porque un abogado revisa la base
 * antes que el total: si los dias estan mal, el peso tambien.
 */
public record SettlementItem(SettlementConcept concept,
                             String label,
                             BigDecimal days,
                             BigDecimal amount) {
}
