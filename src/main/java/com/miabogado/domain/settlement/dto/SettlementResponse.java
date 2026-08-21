package com.miabogado.domain.settlement.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Liquidacion guardada. Devuelve la entrada junto al resultado: sin ella el
 * abogado no puede defender la cifra delante del cliente ni del juez.
 */
public record SettlementResponse(UUID id,
                                 UUID clientId,
                                 String clientName,
                                 UUID legalCaseId,
                                 String caseNumber,
                                 String createdByName,
                                 SettlementRequest input,
                                 SettlementResult result,
                                 Instant createdAt) {
}
