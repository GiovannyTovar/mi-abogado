package com.miabogado.domain.settlement.service;

import com.miabogado.domain.client.entity.Client;
import com.miabogado.domain.client.service.ClientService;
import com.miabogado.domain.legalcase.entity.CaseEventType;
import com.miabogado.domain.legalcase.entity.LegalCase;
import com.miabogado.domain.legalcase.service.CaseEventService;
import com.miabogado.domain.legalcase.service.LegalCaseService;
import com.miabogado.domain.settlement.dto.SaveSettlementRequest;
import com.miabogado.domain.settlement.dto.SettlementConcept;
import com.miabogado.domain.settlement.dto.SettlementItem;
import com.miabogado.domain.settlement.dto.SettlementRequest;
import com.miabogado.domain.settlement.dto.SettlementResponse;
import com.miabogado.domain.settlement.dto.SettlementResult;
import com.miabogado.domain.settlement.dto.SettlementSummary;
import com.miabogado.domain.settlement.entity.LegalParameter;
import com.miabogado.domain.settlement.entity.SettlementCalculation;
import com.miabogado.domain.settlement.repository.SettlementCalculationRepository;
import com.miabogado.domain.user.entity.User;
import com.miabogado.domain.user.repository.UserRepository;
import com.miabogado.shared.error.BusinessException;
import com.miabogado.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso de la calculadora de liquidacion.
 * <p>
 * El calculo lo hace {@link SettlementCalculator}, que no sabe de BD. Este
 * servicio decide con que parametros se calcula, a quien pertenece el resultado
 * y que se guarda.
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final BigDecimal DAYS_IN_MONTH = new BigDecimal("30");

    private final SettlementCalculationRepository settlementRepository;
    private final LegalParameterService legalParameterService;
    private final SettlementCalculator calculator;
    private final ClientService clientService;
    private final LegalCaseService legalCaseService;
    private final CaseEventService caseEventService;
    private final UserRepository userRepository;

    /**
     * Calculo sin guardar. Es lo que responde la calculadora publica y tambien lo
     * que usa la firma para tantear antes de decidir si conserva el resultado.
     */
    @Transactional(readOnly = true)
    public SettlementResult preview(SettlementRequest request) {
        return calculator.calculate(request, parameterFor(request));
    }

    @Transactional(readOnly = true)
    public Page<SettlementSummary> search(UUID clientId, UUID caseId, Pageable pageable) {
        return settlementRepository.search(clientId, caseId, pageable);
    }

    @Transactional(readOnly = true)
    public SettlementResponse findById(UUID id) {
        return toResponse(requireSettlement(id));
    }

    /**
     * Calcula y conserva. Si va contra un expediente queda anotado en la bitacora:
     * la cifra que la firma le puso al cliente es un hecho del caso, no un apunte
     * suelto.
     */
    @Transactional
    public SettlementResponse create(SaveSettlementRequest request) {
        SettlementRequest input = request.input();
        LegalParameter parameter = parameterFor(input);
        SettlementResult result = calculator.calculate(input, parameter);

        LegalCase legalCase = request.legalCaseId() == null
                ? null
                : legalCaseService.requireCase(request.legalCaseId());
        Client client = resolveClient(request, legalCase);

        SettlementCalculation calculation = new SettlementCalculation(client, legalCase, currentUser());
        applyInput(calculation, input);
        applyResult(calculation, result);

        SettlementCalculation saved = settlementRepository.save(calculation);

        if (legalCase != null) {
            caseEventService.record(legalCase, CaseEventType.NOTA,
                    "Liquidacion calculada",
                    "Total estimado: " + formatCurrency(result.total())
                            + " (parametros " + result.parameterYear() + ")");
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        settlementRepository.delete(requireSettlement(id));
    }

    /**
     * Los parametros son los del ano de la terminacion, no los de hoy: una
     * liquidacion de 2022 se calcula con el salario minimo de 2022.
     */
    private LegalParameter parameterFor(SettlementRequest request) {
        return legalParameterService.resolveForYear(request.endDate().getYear());
    }

    /**
     * El cliente sale del expediente cuando hay expediente: pedir los dos y que no
     * coincidan solo permite guardar una liquidacion imposible.
     */
    private Client resolveClient(SaveSettlementRequest request, LegalCase legalCase) {
        if (legalCase != null) {
            if (request.clientId() != null && !request.clientId().equals(legalCase.getClient().getId())) {
                throw BusinessException.conflict("El cliente indicado no es el del expediente");
            }
            return legalCase.getClient();
        }
        return request.clientId() == null ? null : clientService.requireClient(request.clientId());
    }

    private void applyInput(SettlementCalculation calculation, SettlementRequest input) {
        calculation.setEmployeeName(input.employeeName());
        calculation.setContractType(input.contractType());
        calculation.setTerminationReason(input.terminationReason());
        calculation.setMonthlySalary(input.monthlySalary());
        calculation.setVariableAverage(input.variableAverage());
        calculation.setStartDate(input.startDate());
        calculation.setEndDate(input.endDate());
        calculation.setContractEndDate(input.contractEndDate());
        calculation.setTransportAllowanceApplies(input.transportAllowanceApplies());
        calculation.setSeverancePaidThrough(input.severancePaidThrough());
        calculation.setServiceBonusPaidThrough(input.serviceBonusPaidThrough());
        calculation.setVacationDaysTaken(input.vacationDaysTaken());
        calculation.setNotes(input.notes());
    }

    private void applyResult(SettlementCalculation calculation, SettlementResult result) {
        calculation.setParameterYear(result.parameterYear());
        calculation.setMinimumWage(result.minimumWage());
        calculation.setTransportAllowance(result.transportAllowance());
        calculation.setDaysWorked(result.daysWorked());
        calculation.setSeveranceDays(days(result, SettlementConcept.CESANTIAS).intValue());
        calculation.setSeverance(amount(result, SettlementConcept.CESANTIAS));
        calculation.setSeveranceInterest(amount(result, SettlementConcept.INTERESES_CESANTIAS));
        calculation.setServiceBonusDays(days(result, SettlementConcept.PRIMA_SERVICIOS).intValue());
        calculation.setServiceBonus(amount(result, SettlementConcept.PRIMA_SERVICIOS));
        calculation.setVacationDays(days(result, SettlementConcept.VACACIONES));
        calculation.setVacation(amount(result, SettlementConcept.VACACIONES));
        calculation.setIndemnityDays(days(result, SettlementConcept.INDEMNIZACION));
        calculation.setIndemnity(amount(result, SettlementConcept.INDEMNIZACION));
        calculation.setTotal(result.total());
    }

    private BigDecimal days(SettlementResult result, SettlementConcept concept) {
        return find(result, concept).map(SettlementItem::days).orElse(BigDecimal.ZERO);
    }

    private BigDecimal amount(SettlementResult result, SettlementConcept concept) {
        return find(result, concept).map(SettlementItem::amount).orElse(BigDecimal.ZERO);
    }

    private Optional<SettlementItem> find(SettlementResult result, SettlementConcept concept) {
        return result.items().stream().filter(item -> item.concept() == concept).findFirst();
    }

    private SettlementResponse toResponse(SettlementCalculation calculation) {
        return new SettlementResponse(
                calculation.getId(),
                calculation.getClient() == null ? null : calculation.getClient().getId(),
                calculation.getClient() == null ? null : calculation.getClient().getName(),
                calculation.getLegalCase() == null ? null : calculation.getLegalCase().getId(),
                calculation.getLegalCase() == null ? null : calculation.getLegalCase().getCaseNumber(),
                calculation.getCreatedBy() == null ? null : calculation.getCreatedBy().getFullName(),
                toInput(calculation),
                toResult(calculation),
                calculation.getCreatedAt());
    }

    private SettlementRequest toInput(SettlementCalculation calculation) {
        return new SettlementRequest(
                calculation.getEmployeeName(),
                calculation.getContractType(),
                calculation.getTerminationReason(),
                calculation.getMonthlySalary(),
                calculation.getVariableAverage(),
                calculation.getStartDate(),
                calculation.getEndDate(),
                calculation.getContractEndDate(),
                calculation.isTransportAllowanceApplies(),
                calculation.getSeverancePaidThrough(),
                calculation.getServiceBonusPaidThrough(),
                calculation.getVacationDaysTaken(),
                calculation.getNotes());
    }

    /**
     * Rearma el detalle desde las columnas guardadas. No vuelve a calcular: lo que
     * se entrego al cliente es lo que se devuelve, aunque hoy la formula diera otra
     * cosa.
     */
    private SettlementResult toResult(SettlementCalculation calculation) {
        BigDecimal monthlyBase = calculation.getMonthlySalary().add(calculation.getVariableAverage());
        List<SettlementItem> items = new ArrayList<>();
        items.add(new SettlementItem(SettlementConcept.CESANTIAS, "Cesantias",
                BigDecimal.valueOf(calculation.getSeveranceDays()), calculation.getSeverance()));
        items.add(new SettlementItem(SettlementConcept.INTERESES_CESANTIAS, "Intereses sobre cesantias",
                BigDecimal.valueOf(calculation.getSeveranceDays()), calculation.getSeveranceInterest()));
        items.add(new SettlementItem(SettlementConcept.PRIMA_SERVICIOS, "Prima de servicios",
                BigDecimal.valueOf(calculation.getServiceBonusDays()), calculation.getServiceBonus()));
        items.add(new SettlementItem(SettlementConcept.VACACIONES, "Vacaciones pendientes",
                calculation.getVacationDays(), calculation.getVacation()));
        if (calculation.getIndemnityDays().signum() > 0) {
            items.add(new SettlementItem(SettlementConcept.INDEMNIZACION,
                    "Indemnizacion por despido sin justa causa",
                    calculation.getIndemnityDays(), calculation.getIndemnity()));
        }
        return new SettlementResult(
                calculation.getParameterYear(),
                calculation.getMinimumWage(),
                calculation.getTransportAllowance(),
                monthlyBase,
                monthlyBase.divide(DAYS_IN_MONTH, 2, RoundingMode.HALF_UP),
                calculation.getDaysWorked(),
                List.copyOf(items),
                calculation.getTotal());
    }

    /** Null cuando el calculo lo dispara algo sin sesion; hoy siempre hay usuario. */
    private User currentUser() {
        return CurrentUser.find()
                .flatMap(principal -> userRepository.findById(principal.userId()))
                .orElse(null);
    }

    private String formatCurrency(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.of("es", "CO")).format(amount);
    }

    private SettlementCalculation requireSettlement(UUID id) {
        return settlementRepository.findWithDetailsById(id)
                .orElseThrow(() -> BusinessException.notFound("Liquidacion"));
    }
}
