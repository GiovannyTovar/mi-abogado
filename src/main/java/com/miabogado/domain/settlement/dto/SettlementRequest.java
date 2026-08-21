package com.miabogado.domain.settlement.dto;

import com.miabogado.domain.settlement.entity.ContractType;
import com.miabogado.domain.settlement.entity.TerminationReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos del contrato que se liquida. Es la misma entrada para la calculadora
 * publica y para la de la firma: el calculo no cambia segun quien pregunte.
 *
 * @param variableAverage         promedio mensual de comisiones, recargos y horas
 *                                extra. Entra a la base de prestaciones (art. 127 CST).
 * @param contractEndDate         fin pactado del contrato a termino fijo o de la obra.
 *                                Obligatorio para calcular la indemnizacion de esas
 *                                modalidades: sin el no se sabe cuanto faltaba.
 * @param severancePaidThrough    fecha hasta la que ya se consignaron cesantias al
 *                                fondo (tipicamente el 31 de diciembre anterior).
 *                                Null = se deben desde el ingreso.
 * @param serviceBonusPaidThrough fecha de la ultima prima pagada. Null = se liquida
 *                                el semestre en curso, que es el caso normal.
 * @param vacationDaysTaken       dias de vacaciones ya disfrutados o compensados.
 */
public record SettlementRequest(
        @NotBlank @Size(max = 180) String employeeName,
        @NotNull ContractType contractType,
        @NotNull TerminationReason terminationReason,
        @NotNull @DecimalMin(value = "1", message = "El salario debe ser mayor que cero") BigDecimal monthlySalary,
        @PositiveOrZero BigDecimal variableAverage,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        LocalDate contractEndDate,
        Boolean transportAllowanceApplies,
        LocalDate severancePaidThrough,
        LocalDate serviceBonusPaidThrough,
        @PositiveOrZero BigDecimal vacationDaysTaken,
        @Size(max = 2000) String notes) {

    /** Los opcionales se normalizan aqui para que ni el motor ni la entidad vean nulls. */
    public SettlementRequest {
        variableAverage = variableAverage == null ? BigDecimal.ZERO : variableAverage;
        vacationDaysTaken = vacationDaysTaken == null ? BigDecimal.ZERO : vacationDaysTaken;
        transportAllowanceApplies = transportAllowanceApplies == null || transportAllowanceApplies;
    }
}
