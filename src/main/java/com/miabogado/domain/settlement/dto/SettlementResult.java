package com.miabogado.domain.settlement.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado del calculo.
 *
 * @param parameterYear      ano de los parametros usados. Puede no coincidir con el
 *                           de la terminacion si todavia no se cargaron los del ano
 *                           en curso: el cliente necesita verlo para no fiarse a ciegas.
 * @param monthlyBase        salario mensual + promedio variable, sin auxilio.
 * @param transportAllowance auxilio mensual aplicado; cero si el salario lo supera.
 */
public record SettlementResult(int parameterYear,
                               BigDecimal minimumWage,
                               BigDecimal transportAllowance,
                               BigDecimal monthlyBase,
                               BigDecimal dailySalary,
                               int daysWorked,
                               List<SettlementItem> items,
                               BigDecimal total) {
}
