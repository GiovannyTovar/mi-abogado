package com.miabogado.domain.settlement.entity;

import com.miabogado.domain.client.entity.Client;
import com.miabogado.domain.legalcase.entity.LegalCase;
import com.miabogado.domain.user.entity.User;
import com.miabogado.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una liquidacion guardada por la firma.
 * <p>
 * Guarda la entrada <b>y</b> el resultado. No se recalcula al leer: si manana
 * se corrige una formula o se carga otro parametro legal, la liquidacion que la
 * firma ya le entrego al cliente tiene que seguir diciendo lo mismo. Por eso
 * tampoco se edita: se calcula otra vez y se guarda de nuevo.
 */
@Entity
@Table(name = "settlement_calculation")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementCalculation extends TenantScopedEntity {

    /** Opcional: la calculadora tambien sirve para tantear antes de tener cliente. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_case_id")
    private LegalCase legalCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    // ---- entrada -----------------------------------------------------

    @Column(name = "employee_name", nullable = false, length = 180)
    private String employeeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 20)
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(name = "termination_reason", nullable = false, length = 30)
    private TerminationReason terminationReason;

    @Column(name = "monthly_salary", nullable = false)
    private BigDecimal monthlySalary;

    /** Promedio mensual de comisiones, recargos y horas extra. */
    @Column(name = "variable_average", nullable = false)
    private BigDecimal variableAverage = BigDecimal.ZERO;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Fin pactado del contrato a termino fijo, o de la obra. */
    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "transport_allowance_applies", nullable = false)
    private boolean transportAllowanceApplies = true;

    @Column(name = "severance_paid_through")
    private LocalDate severancePaidThrough;

    @Column(name = "service_bonus_paid_through")
    private LocalDate serviceBonusPaidThrough;

    @Column(name = "vacation_days_taken", nullable = false)
    private BigDecimal vacationDaysTaken = BigDecimal.ZERO;

    @Column(columnDefinition = "text")
    private String notes;

    // ---- resultado congelado -----------------------------------------

    @Column(name = "parameter_year", nullable = false)
    private int parameterYear;

    /**
     * SMLMV y auxilio con los que se calculo. Se copian aqui: si manana el
     * super-admin corrige la cifra de ese ano, esta liquidacion tiene que seguir
     * explicando de donde salio su total.
     */
    @Column(name = "minimum_wage", nullable = false)
    private BigDecimal minimumWage;

    @Column(name = "transport_allowance", nullable = false)
    private BigDecimal transportAllowance;

    @Column(name = "days_worked", nullable = false)
    private int daysWorked;

    @Column(name = "severance_days", nullable = false)
    private int severanceDays;

    @Column(nullable = false)
    private BigDecimal severance;

    @Column(name = "severance_interest", nullable = false)
    private BigDecimal severanceInterest;

    @Column(name = "service_bonus_days", nullable = false)
    private int serviceBonusDays;

    @Column(name = "service_bonus", nullable = false)
    private BigDecimal serviceBonus;

    @Column(name = "vacation_days", nullable = false)
    private BigDecimal vacationDays;

    @Column(nullable = false)
    private BigDecimal vacation;

    @Column(name = "indemnity_days", nullable = false)
    private BigDecimal indemnityDays;

    @Column(nullable = false)
    private BigDecimal indemnity;

    @Column(nullable = false)
    private BigDecimal total;

    /**
     * Cliente y expediente son opcionales; el resto de campos los llena el
     * servicio a partir del calculo, que es quien sabe cuales van juntos.
     */
    public SettlementCalculation(Client client, LegalCase legalCase, User createdBy) {
        this.client = client;
        this.legalCase = legalCase;
        this.createdBy = createdBy;
    }
}
