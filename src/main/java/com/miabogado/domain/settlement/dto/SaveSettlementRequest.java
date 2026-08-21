package com.miabogado.domain.settlement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Calculo que la firma quiere conservar. Cliente y expediente son opcionales:
 * la firma tantea la cifra antes de que el lead sea cliente.
 */
public record SaveSettlementRequest(UUID clientId,
                                    UUID legalCaseId,
                                    @NotNull @Valid SettlementRequest input) {
}
