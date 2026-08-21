package com.miabogado.domain.settlement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Alta o actualizacion de los parametros de un ano. Es un upsert por ano y no
 * un POST + PATCH porque en diciembre solo interesa una cosa: que las cifras
 * del ano entrante queden cargadas, exista o no ya la fila.
 * <p>
 * Los tres ultimos campos son opcionales: cambian de decada en decada, no de
 * ano en ano, y null deja el valor vigente.
 */
public record UpsertLegalParameterRequest(
        @NotNull @Min(1990) @Max(2100) Integer year,
        @NotNull @DecimalMin(value = "1", message = "El salario minimo debe ser mayor que cero") BigDecimal minimumWage,
        @NotNull @PositiveOrZero BigDecimal transportAllowance,
        @NotNull @DecimalMin(value = "1", message = "La UVT debe ser mayor que cero") BigDecimal uvt,
        @PositiveOrZero BigDecimal severanceInterestRate,
        @DecimalMin(value = "0.01") BigDecimal transportAllowanceWageCap,
        @DecimalMin(value = "0.01") BigDecimal highSalaryThreshold) {
}
