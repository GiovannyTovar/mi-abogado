package com.miabogado.domain.settlement.entity;

import com.miabogado.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Cifras legales de un ano. Catalogo de plataforma, no de firma: el salario
 * minimo es el mismo para todos y la calculadora publica lo lee sin sesion.
 * <p>
 * Vive en el dominio {@code settlement} porque existe para el, igual que
 * {@code PracticeArea} vive en {@code lawyer}.
 */
@Entity
@Table(name = "legal_parameter")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalParameter extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private int year;

    /** SMLMV. */
    @Column(name = "minimum_wage", nullable = false)
    private BigDecimal minimumWage;

    @Column(name = "transport_allowance", nullable = false)
    private BigDecimal transportAllowance;

    @Column(nullable = false)
    private BigDecimal uvt;

    /** Interes anual sobre cesantias: 0.12. */
    @Column(name = "severance_interest_rate", nullable = false)
    private BigDecimal severanceInterestRate = new BigDecimal("0.1200");

    /** Tope en SMLMV hasta el que se causa auxilio de transporte: 2. */
    @Column(name = "transport_allowance_wage_cap", nullable = false)
    private BigDecimal transportAllowanceWageCap = new BigDecimal("2.00");

    /** Umbral en SMLMV a partir del cual la indemnizacion baja a 20+15 dias: 10. */
    @Column(name = "high_salary_threshold", nullable = false)
    private BigDecimal highSalaryThreshold = new BigDecimal("10.00");

    public LegalParameter(int year, BigDecimal minimumWage, BigDecimal transportAllowance, BigDecimal uvt) {
        this.year = year;
        this.minimumWage = minimumWage;
        this.transportAllowance = transportAllowance;
        this.uvt = uvt;
    }

    /** Salario a partir del cual ya no se causa auxilio de transporte. */
    public BigDecimal transportAllowanceCeiling() {
        return minimumWage.multiply(transportAllowanceWageCap);
    }

    /** Salario a partir del cual la indemnizacion del art. 64 CST es de 20+15 dias. */
    public BigDecimal highSalaryCeiling() {
        return minimumWage.multiply(highSalaryThreshold);
    }
}
