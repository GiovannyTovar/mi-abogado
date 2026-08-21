package com.miabogado.domain.settlement.service;

import com.miabogado.domain.settlement.dto.SettlementConcept;
import com.miabogado.domain.settlement.dto.SettlementItem;
import com.miabogado.domain.settlement.dto.SettlementRequest;
import com.miabogado.domain.settlement.dto.SettlementResult;
import com.miabogado.domain.settlement.entity.LegalParameter;
import com.miabogado.domain.settlement.entity.TerminationReason;
import com.miabogado.shared.error.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de liquidacion laboral colombiana.
 * <p>
 * Es una funcion pura: entrada + parametros del ano -> resultado. No toca la BD,
 * no mira el tenant y no sabe quien pregunta. Por eso la calculadora publica y
 * la de la firma dan exactamente la misma cifra, y por eso se puede probar sin
 * levantar Spring ni Postgres.
 *
 * <h2>Reglas implementadas</h2>
 * <ul>
 *   <li><b>Cesantias</b> (art. 249 CST): base * dias / 360.</li>
 *   <li><b>Intereses sobre cesantias</b> (art. 99 Ley 50/1990): cesantias * dias * 12% / 360.</li>
 *   <li><b>Prima de servicios</b> (art. 306 CST): base * dias del semestre / 360.</li>
 *   <li><b>Vacaciones</b> (art. 186 CST): 15 dias por ano, <b>sin</b> auxilio de transporte.</li>
 *   <li><b>Indemnizacion</b> (art. 64 CST): solo si el despido fue sin justa causa.</li>
 * </ul>
 * La base de cesantias y prima incluye el auxilio de transporte (art. 7 Ley 1 de 1963);
 * la de vacaciones e indemnizacion, no: el auxilio no es salario.
 *
 * <h2>Fuera de alcance, a proposito</h2>
 * Salario integral, indemnizacion moratoria del art. 65 CST y aportes a seguridad
 * social. Cada uno depende de hechos que esta entrada no recoge (fecha de pago,
 * pacto de integralidad); calcularlos a ciegas daria una cifra falsa con aspecto
 * de cierta.
 */
@Component
public class SettlementCalculator {

    /** El ano comercial de 360 dias es el que manda en materia laboral. */
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("360");
    private static final BigDecimal DAYS_IN_MONTH = new BigDecimal("30");
    /** 15 dias de vacaciones por ano trabajado. */
    private static final BigDecimal VACATION_DAYS_PER_YEAR = new BigDecimal("15");
    private static final BigDecimal MINIMUM_REMAINING_TERM_DAYS = new BigDecimal("15");
    private static final int MONEY_SCALE = 2;
    private static final int RATIO_SCALE = 10;

    public SettlementResult calculate(SettlementRequest request, LegalParameter parameter) {
        validate(request);

        BigDecimal monthlyBase = request.monthlySalary().add(request.variableAverage());
        BigDecimal transportAllowance = transportAllowance(request, parameter, monthlyBase);
        BigDecimal provisionBase = monthlyBase.add(transportAllowance);
        BigDecimal dailySalary = monthlyBase.divide(DAYS_IN_MONTH, RATIO_SCALE, RoundingMode.HALF_UP);

        int daysWorked = commercialDays(request.startDate(), request.endDate());
        List<SettlementItem> items = new ArrayList<>();

        int severanceDays = commercialDays(from(request.startDate(), request.severancePaidThrough()), request.endDate());
        BigDecimal severance = money(proportional(provisionBase, severanceDays));
        items.add(new SettlementItem(SettlementConcept.CESANTIAS,
                "Cesantias", BigDecimal.valueOf(severanceDays), severance));

        // El interes corre sobre las cesantias del periodo, no sobre las ya consignadas.
        BigDecimal severanceInterest = money(severance
                .multiply(BigDecimal.valueOf(severanceDays))
                .multiply(parameter.getSeveranceInterestRate())
                .divide(DAYS_IN_YEAR, RATIO_SCALE, RoundingMode.HALF_UP));
        items.add(new SettlementItem(SettlementConcept.INTERESES_CESANTIAS,
                "Intereses sobre cesantias", BigDecimal.valueOf(severanceDays), severanceInterest));

        int serviceBonusDays = commercialDays(serviceBonusFrom(request), request.endDate());
        BigDecimal serviceBonus = money(proportional(provisionBase, serviceBonusDays));
        items.add(new SettlementItem(SettlementConcept.PRIMA_SERVICIOS,
                "Prima de servicios", BigDecimal.valueOf(serviceBonusDays), serviceBonus));

        BigDecimal vacationDays = pendingVacationDays(request, daysWorked);
        BigDecimal vacation = money(dailySalary.multiply(vacationDays));
        items.add(new SettlementItem(SettlementConcept.VACACIONES,
                "Vacaciones pendientes", vacationDays, vacation));

        BigDecimal indemnityDays = indemnityDays(request, parameter, monthlyBase, daysWorked);
        if (indemnityDays.signum() > 0) {
            items.add(new SettlementItem(SettlementConcept.INDEMNIZACION,
                    "Indemnizacion por despido sin justa causa",
                    indemnityDays, money(dailySalary.multiply(indemnityDays))));
        }

        BigDecimal total = items.stream()
                .map(SettlementItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SettlementResult(parameter.getYear(),
                parameter.getMinimumWage(),
                transportAllowance,
                money(monthlyBase),
                money(dailySalary),
                daysWorked,
                List.copyOf(items),
                total);
    }

    private void validate(SettlementRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw BusinessException.conflict("La fecha de retiro no puede ser anterior a la de ingreso");
        }
        if (request.severancePaidThrough() != null
                && request.severancePaidThrough().isAfter(request.endDate())) {
            throw BusinessException.conflict("Las cesantias no pueden estar pagadas mas alla del retiro");
        }
        if (request.serviceBonusPaidThrough() != null
                && request.serviceBonusPaidThrough().isAfter(request.endDate())) {
            throw BusinessException.conflict("La prima no puede estar pagada mas alla del retiro");
        }
        if (request.contractEndDate() != null && request.contractEndDate().isBefore(request.startDate())) {
            throw BusinessException.conflict("El fin pactado del contrato no puede ser anterior al ingreso");
        }
    }

    /**
     * El auxilio de transporte solo se causa hasta dos salarios minimos, y el tope
     * se mide contra el salario, no contra el salario mas el auxilio.
     */
    private BigDecimal transportAllowance(SettlementRequest request, LegalParameter parameter, BigDecimal monthlyBase) {
        boolean withinCap = monthlyBase.compareTo(parameter.transportAllowanceCeiling()) <= 0;
        return request.transportAllowanceApplies() && withinCap
                ? parameter.getTransportAllowance()
                : BigDecimal.ZERO;
    }

    /** Si ya se pago hasta cierta fecha, el periodo pendiente empieza al dia siguiente. */
    private LocalDate from(LocalDate startDate, LocalDate paidThrough) {
        if (paidThrough == null || paidThrough.isBefore(startDate)) {
            return startDate;
        }
        return paidThrough.plusDays(1);
    }

    /**
     * Sin dato de la ultima prima se liquida el semestre en curso, que es el caso
     * normal: la prima se paga en junio y en diciembre, asi que al retirarse solo
     * queda pendiente la fraccion del semestre corriente.
     */
    private LocalDate serviceBonusFrom(SettlementRequest request) {
        if (request.serviceBonusPaidThrough() != null) {
            return from(request.startDate(), request.serviceBonusPaidThrough());
        }
        LocalDate end = request.endDate();
        LocalDate semesterStart = end.getMonthValue() <= 6
                ? LocalDate.of(end.getYear(), 1, 1)
                : LocalDate.of(end.getYear(), 7, 1);
        return semesterStart.isAfter(request.startDate()) ? semesterStart : request.startDate();
    }

    private BigDecimal pendingVacationDays(SettlementRequest request, int daysWorked) {
        BigDecimal accrued = BigDecimal.valueOf(daysWorked)
                .multiply(VACATION_DAYS_PER_YEAR)
                .divide(DAYS_IN_YEAR, MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal pending = accrued.subtract(request.vacationDaysTaken());
        return pending.signum() > 0 ? pending : BigDecimal.ZERO;
    }

    /**
     * Art. 64 CST. Devuelve cero salvo despido sin justa causa: las prestaciones
     * se deben siempre, la indemnizacion solo cuando el empleador rompio el contrato.
     */
    private BigDecimal indemnityDays(SettlementRequest request, LegalParameter parameter,
                                     BigDecimal monthlyBase, int daysWorked) {
        if (request.terminationReason() != TerminationReason.SIN_JUSTA_CAUSA) {
            return BigDecimal.ZERO;
        }
        return switch (request.contractType()) {
            case INDEFINIDO -> indefiniteTermIndemnityDays(parameter, monthlyBase, daysWorked);
            // Fijo y obra se indemnizan igual: lo que faltaba para terminar, nunca
            // menos de 15 dias (art. 64, incisos 2 y 3).
            case FIJO, OBRA_LABOR -> remainingTermIndemnityDays(request);
        };
    }

    /**
     * Hasta el umbral (10 SMLMV): 30 dias por el primer ano y 20 por cada ano
     * siguiente. Por encima: 20 y 15. El primer ano se paga completo aunque se
     * haya trabajado menos; lo que va despues es proporcional.
     */
    private BigDecimal indefiniteTermIndemnityDays(LegalParameter parameter, BigDecimal monthlyBase, int daysWorked) {
        boolean highSalary = monthlyBase.compareTo(parameter.highSalaryCeiling()) >= 0;
        BigDecimal firstYear = highSalary ? new BigDecimal("20") : new BigDecimal("30");
        BigDecimal perExtraYear = highSalary ? new BigDecimal("15") : new BigDecimal("20");

        if (daysWorked <= DAYS_IN_YEAR.intValue()) {
            return firstYear;
        }
        BigDecimal extraDays = BigDecimal.valueOf(daysWorked).subtract(DAYS_IN_YEAR);
        return firstYear.add(extraDays.multiply(perExtraYear)
                .divide(DAYS_IN_YEAR, MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private BigDecimal remainingTermIndemnityDays(SettlementRequest request) {
        if (request.contractEndDate() == null) {
            throw BusinessException.conflict(
                    "Para indemnizar un contrato a termino fijo o de obra hace falta la fecha pactada de terminacion");
        }
        int remaining = commercialDays(request.endDate().plusDays(1), request.contractEndDate());
        return BigDecimal.valueOf(remaining).max(MINIMUM_REMAINING_TERM_DAYS);
    }

    private BigDecimal proportional(BigDecimal monthlyBase, int days) {
        return monthlyBase.multiply(BigDecimal.valueOf(days))
                .divide(DAYS_IN_YEAR, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Dias del ano comercial de 360, contando el dia de ingreso y el de retiro.
     * Todo mes vale 30: ni febrero acorta la liquidacion ni los meses de 31 la
     * alargan. Es la cuenta con la que se liquida en Colombia.
     */
    static int commercialDays(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            return 0;
        }
        int startDay = Math.min(start.getDayOfMonth(), 30);
        int endDay = end.getDayOfMonth() == end.lengthOfMonth() ? 30 : Math.min(end.getDayOfMonth(), 30);
        return (end.getYear() - start.getYear()) * 360
                + (end.getMonthValue() - start.getMonthValue()) * 30
                + (endDay - startDay)
                + 1;
    }
}
