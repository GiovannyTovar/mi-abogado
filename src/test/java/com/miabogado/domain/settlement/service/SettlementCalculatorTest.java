package com.miabogado.domain.settlement.service;

import com.miabogado.domain.settlement.dto.SettlementConcept;
import com.miabogado.domain.settlement.dto.SettlementItem;
import com.miabogado.domain.settlement.dto.SettlementRequest;
import com.miabogado.domain.settlement.dto.SettlementResult;
import com.miabogado.domain.settlement.entity.ContractType;
import com.miabogado.domain.settlement.entity.LegalParameter;
import com.miabogado.domain.settlement.entity.TerminationReason;
import com.miabogado.shared.error.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El motor es una funcion pura, asi que se prueba sin Spring, sin Postgres y sin
 * Docker: son las cifras las que hay que verificar, no el cableado.
 */
class SettlementCalculatorTest {

    private static final LegalParameter PARAM_2024 = new LegalParameter(2024,
            new BigDecimal("1300000.00"), new BigDecimal("162000.00"), new BigDecimal("47065.00"));

    private final SettlementCalculator calculator = new SettlementCalculator();

    @Nested
    @DisplayName("Ano comercial de 360 dias")
    class CommercialDays {

        @Test
        void un_ano_calendario_completo_son_360_dias() {
            assertThat(SettlementCalculator.commercialDays(
                    LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))).isEqualTo(360);
        }

        @Test
        void todo_mes_vale_30_dias_aunque_tenga_28_o_31() {
            assertThat(SettlementCalculator.commercialDays(
                    LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 28))).isEqualTo(30);
            assertThat(SettlementCalculator.commercialDays(
                    LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))).isEqualTo(30);
        }

        @Test
        void cuenta_el_dia_de_ingreso_y_el_de_retiro() {
            assertThat(SettlementCalculator.commercialDays(
                    LocalDate.of(2024, 3, 10), LocalDate.of(2024, 3, 10))).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Ano completo, despido sin justa causa, con auxilio de transporte")
    void liquida_un_ano_completo() {
        SettlementResult result = calculator.calculate(request()
                .salary("2000000")
                .from(2024, 1, 1).to(2024, 12, 31)
                .build(), PARAM_2024);

        assertThat(result.daysWorked()).isEqualTo(360);
        assertThat(result.transportAllowance()).isEqualByComparingTo("162000.00");
        // Base de prestaciones: 2.000.000 + 162.000 de auxilio.
        assertThat(amount(result, SettlementConcept.CESANTIAS)).isEqualByComparingTo("2162000.00");
        assertThat(amount(result, SettlementConcept.INTERESES_CESANTIAS)).isEqualByComparingTo("259440.00");
        // Sin dato de la ultima prima se liquida el semestre en curso: 180 dias.
        assertThat(days(result, SettlementConcept.PRIMA_SERVICIOS)).isEqualByComparingTo("180");
        assertThat(amount(result, SettlementConcept.PRIMA_SERVICIOS)).isEqualByComparingTo("1081000.00");
        // Vacaciones e indemnizacion van sin auxilio: no es salario.
        assertThat(days(result, SettlementConcept.VACACIONES)).isEqualByComparingTo("15.00");
        assertThat(amount(result, SettlementConcept.VACACIONES)).isEqualByComparingTo("1000000.00");
        assertThat(days(result, SettlementConcept.INDEMNIZACION)).isEqualByComparingTo("30");
        assertThat(amount(result, SettlementConcept.INDEMNIZACION)).isEqualByComparingTo("2000000.00");
        assertThat(result.total()).isEqualByComparingTo("6502440.00");
    }

    @Test
    @DisplayName("Sobre dos salarios minimos no se causa auxilio de transporte")
    void sin_auxilio_por_encima_del_tope() {
        SettlementResult result = calculator.calculate(request()
                .salary("5000000")
                .from(2024, 1, 1).to(2024, 12, 31)
                .build(), PARAM_2024);

        assertThat(result.transportAllowance()).isEqualByComparingTo("0");
        assertThat(amount(result, SettlementConcept.CESANTIAS)).isEqualByComparingTo("5000000.00");
    }

    @Test
    @DisplayName("El promedio de lo variable entra a la base de prestaciones")
    void el_promedio_variable_entra_a_la_base() {
        SettlementResult result = calculator.calculate(request()
                .salary("2000000")
                .variable("500000")
                .from(2024, 1, 1).to(2024, 12, 31)
                .build(), PARAM_2024);

        assertThat(result.monthlyBase()).isEqualByComparingTo("2500000.00");
        assertThat(amount(result, SettlementConcept.CESANTIAS)).isEqualByComparingTo("2662000.00");
    }

    @Test
    @DisplayName("Quien renuncia cobra prestaciones pero no indemnizacion")
    void la_renuncia_no_causa_indemnizacion() {
        SettlementResult result = calculator.calculate(request()
                .salary("2000000")
                .reason(TerminationReason.RENUNCIA)
                .from(2024, 1, 1).to(2024, 12, 31)
                .build(), PARAM_2024);

        assertThat(result.items()).extracting(SettlementItem::concept)
                .doesNotContain(SettlementConcept.INDEMNIZACION);
        assertThat(amount(result, SettlementConcept.CESANTIAS)).isEqualByComparingTo("2162000.00");
    }

    @Test
    @DisplayName("Menos de un ano: la indemnizacion del primer ano se paga completa")
    void primer_ano_completo_aunque_se_trabaje_menos() {
        SettlementResult result = calculator.calculate(request()
                .salary("2000000")
                .from(2024, 7, 1).to(2024, 12, 31)
                .build(), PARAM_2024);

        assertThat(result.daysWorked()).isEqualTo(180);
        assertThat(days(result, SettlementConcept.INDEMNIZACION)).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("Tres anos: 30 dias del primero mas 20 por cada ano siguiente")
    void anos_siguientes_a_20_dias() {
        SettlementResult result = calculator.calculate(request()
                .salary("2000000")
                .from(2022, 1, 1).to(2024, 12, 31)
                .build(), PARAM_2024);

        assertThat(result.daysWorked()).isEqualTo(1080);
        assertThat(days(result, SettlementConcept.INDEMNIZACION)).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("Desde 10 SMLMV la indemnizacion baja a 20 dias mas 15 por ano")
    void salario_alto_indemniza_menos() {
        SettlementResult result = calculator.calculate(request()
                .salary("15000000")
                .from(2022, 1, 1).to(2024, 12, 31)
                .build(), PARAM_2024);

        assertThat(days(result, SettlementConcept.INDEMNIZACION)).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Termino fijo: se indemnizan los salarios que faltaban")
    void termino_fijo_paga_lo_que_faltaba() {
        SettlementResult result = calculator.calculate(request()
                .salary("2000000")
                .contract(ContractType.FIJO)
                .contractEnd(2025, 6, 30)
                .from(2024, 1, 1).to(2024, 12, 31)
                .build(), PARAM_2024);

        assertThat(days(result, SettlementConcept.INDEMNIZACION)).isEqualByComparingTo("180");
    }

    @Test
    @DisplayName("Termino fijo: nunca menos de 15 dias")
    void termino_fijo_tiene_piso_de_15_dias() {
        SettlementResult result = calculator.calculate(request()
                .salary("2000000")
                .contract(ContractType.FIJO)
                .contractEnd(2025, 1, 5)
                .from(2024, 1, 1).to(2024, 12, 31)
                .build(), PARAM_2024);

        assertThat(days(result, SettlementConcept.INDEMNIZACION)).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("Termino fijo sin fecha pactada: no se puede saber cuanto faltaba")
    void termino_fijo_exige_fecha_pactada() {
        SettlementRequest request = request()
                .salary("2000000")
                .contract(ContractType.FIJO)
                .from(2024, 1, 1).to(2024, 12, 31)
                .build();

        assertThatThrownBy(() -> calculator.calculate(request, PARAM_2024))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("fecha pactada");
    }

    @Test
    @DisplayName("Las cesantias ya consignadas no se vuelven a deber")
    void solo_se_deben_las_cesantias_no_consignadas() {
        SettlementResult result = calculator.calculate(request()
                .salary("2000000")
                .severancePaidThrough(2023, 12, 31)
                .from(2022, 1, 1).to(2024, 6, 30)
                .build(), PARAM_2024);

        assertThat(result.daysWorked()).isEqualTo(900);
        assertThat(days(result, SettlementConcept.CESANTIAS)).isEqualByComparingTo("180");
        assertThat(amount(result, SettlementConcept.CESANTIAS)).isEqualByComparingTo("1081000.00");
    }

    @Test
    @DisplayName("Las vacaciones ya disfrutadas se descuentan y nunca quedan en negativo")
    void descuenta_las_vacaciones_disfrutadas() {
        SettlementResult result = calculator.calculate(request()
                .salary("2000000")
                .vacationDaysTaken("20")
                .from(2024, 1, 1).to(2024, 12, 31)
                .build(), PARAM_2024);

        assertThat(days(result, SettlementConcept.VACACIONES)).isEqualByComparingTo("0");
        assertThat(amount(result, SettlementConcept.VACACIONES)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Retirarse antes de entrar no es una liquidacion")
    void rechaza_fechas_invertidas() {
        SettlementRequest request = request()
                .salary("2000000")
                .from(2024, 12, 31).to(2024, 1, 1)
                .build();

        assertThatThrownBy(() -> calculator.calculate(request, PARAM_2024))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("anterior a la de ingreso");
    }

    private BigDecimal amount(SettlementResult result, SettlementConcept concept) {
        return item(result, concept).amount();
    }

    private BigDecimal days(SettlementResult result, SettlementConcept concept) {
        return item(result, concept).days();
    }

    private SettlementItem item(SettlementResult result, SettlementConcept concept) {
        return result.items().stream()
                .filter(candidate -> candidate.concept() == concept)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Falta el renglon " + concept));
    }

    private static Builder request() {
        return new Builder();
    }

    /** Solo para que cada prueba nombre lo que cambia y calle lo que no. */
    private static final class Builder {
        private BigDecimal salary = new BigDecimal("2000000");
        private BigDecimal variable = BigDecimal.ZERO;
        private ContractType contractType = ContractType.INDEFINIDO;
        private TerminationReason reason = TerminationReason.SIN_JUSTA_CAUSA;
        private LocalDate start = LocalDate.of(2024, 1, 1);
        private LocalDate end = LocalDate.of(2024, 12, 31);
        private LocalDate contractEnd;
        private LocalDate severancePaidThrough;
        private BigDecimal vacationDaysTaken = BigDecimal.ZERO;

        Builder salary(String value) {
            this.salary = new BigDecimal(value);
            return this;
        }

        Builder variable(String value) {
            this.variable = new BigDecimal(value);
            return this;
        }

        Builder contract(ContractType value) {
            this.contractType = value;
            return this;
        }

        Builder reason(TerminationReason value) {
            this.reason = value;
            return this;
        }

        Builder from(int year, int month, int day) {
            this.start = LocalDate.of(year, month, day);
            return this;
        }

        Builder to(int year, int month, int day) {
            this.end = LocalDate.of(year, month, day);
            return this;
        }

        Builder contractEnd(int year, int month, int day) {
            this.contractEnd = LocalDate.of(year, month, day);
            return this;
        }

        Builder severancePaidThrough(int year, int month, int day) {
            this.severancePaidThrough = LocalDate.of(year, month, day);
            return this;
        }

        Builder vacationDaysTaken(String value) {
            this.vacationDaysTaken = new BigDecimal(value);
            return this;
        }

        SettlementRequest build() {
            return new SettlementRequest("Trabajador de prueba", contractType, reason, salary, variable,
                    start, end, contractEnd, true, severancePaidThrough, null, vacationDaysTaken, null);
        }
    }
}
