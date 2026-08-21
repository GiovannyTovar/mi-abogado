package com.miabogado.domain.settlement.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LegalParameterResponse(UUID id,
                                     int year,
                                     BigDecimal minimumWage,
                                     BigDecimal transportAllowance,
                                     BigDecimal uvt,
                                     BigDecimal severanceInterestRate,
                                     BigDecimal transportAllowanceWageCap,
                                     BigDecimal highSalaryThreshold) {
}
