package com.mi.abogado.domain.legalcase.service;

import com.mi.abogado.domain.legalcase.dto.CaseDeadlineResponse;
import com.mi.abogado.domain.legalcase.dto.CreateCaseDeadlineRequest;
import com.mi.abogado.domain.legalcase.dto.UpcomingDeadline;
import com.mi.abogado.domain.legalcase.entity.CaseDeadline;
import com.mi.abogado.domain.legalcase.entity.CaseEventType;
import com.mi.abogado.domain.legalcase.entity.LegalCase;
import com.mi.abogado.domain.legalcase.mapper.LegalCaseMapper;
import com.mi.abogado.domain.legalcase.repository.CaseDeadlineRepository;
import com.mi.abogado.domain.legalcase.repository.LegalCaseRepository;
import com.mi.abogado.domain.user.entity.User;
import com.mi.abogado.domain.user.repository.UserRepository;
import com.mi.abogado.shared.error.BusinessException;
import com.mi.abogado.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Terminos procesales y alertas de vencimiento.
 * <p>
 * En materia laboral un termino perdido suele ser el caso perdido, asi que esto
 * no es una agenda decorativa: es el control de riesgo de la firma.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseDeadlineService {

    private final CaseDeadlineRepository deadlineRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final UserRepository userRepository;
    private final CaseEventService caseEventService;
    private final LegalCaseMapper legalCaseMapper;

    @Transactional(readOnly = true)
    public List<CaseDeadlineResponse> findByCase(UUID caseId) {
        return legalCaseMapper.toDeadlineResponses(deadlineRepository.findByLegalCase_IdOrderByDueDateAsc(caseId));
    }

    /**
     * Agenda de la firma. Incluye lo ya vencido y aun pendiente, que es
     * precisamente lo que hay que atender primero.
     *
     * @param withinDays ventana hacia adelante en dias
     * @param lawyerId   null = toda la firma; con valor, la agenda de un abogado
     */
    @Transactional(readOnly = true)
    public List<UpcomingDeadline> findUpcoming(int withinDays, UUID lawyerId) {
        return deadlineRepository.findUpcoming(LocalDate.now().plusDays(withinDays), lawyerId);
    }

    @Transactional
    public CaseDeadlineResponse create(UUID caseId, CreateCaseDeadlineRequest request) {
        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> BusinessException.notFound("Expediente"));

        if (legalCase.isClosed()) {
            throw BusinessException.conflict("No se agregan terminos a un expediente cerrado");
        }

        CaseDeadline deadline = new CaseDeadline(
                legalCase, currentUser(), request.deadlineType(), request.title(), request.dueDate());
        deadline.setDescription(request.description());
        if (request.notifyDaysBefore() != null) {
            deadline.setNotifyDaysBefore(request.notifyDaysBefore());
        }

        deadlineRepository.save(deadline);
        caseEventService.record(legalCase, CaseEventType.ACTUACION,
                "Termino registrado: " + request.title(), "Vence el " + request.dueDate());

        return legalCaseMapper.toResponse(deadline);
    }

    @Transactional
    public CaseDeadlineResponse complete(UUID deadlineId) {
        CaseDeadline deadline = deadlineRepository.findById(deadlineId)
                .orElseThrow(() -> BusinessException.notFound("Termino"));

        if (!deadline.isPending()) {
            throw BusinessException.conflict("El termino ya no esta pendiente");
        }

        deadline.complete(currentUser(), Instant.now());
        caseEventService.record(deadline.getLegalCase(), CaseEventType.ACTUACION,
                "Termino cumplido: " + deadline.getTitle(), null);

        return legalCaseMapper.toResponse(deadline);
    }

    /**
     * Marca como MISSED lo que vencio sin cumplirse. Lo llama el job diario.
     * <p>
     * Cruza todas las firmas a proposito: es tarea de plataforma, no de una firma.
     */
    @Transactional
    public int markOverdueAsMissed() {
        int missed = deadlineRepository.markOverdueAsMissed(LocalDate.now());
        if (missed > 0) {
            log.warn("{} terminos vencieron sin cumplirse", missed);
        }
        return missed;
    }

    private User currentUser() {
        return CurrentUser.find()
                .flatMap(principal -> userRepository.findById(principal.userId()))
                .orElse(null);
    }
}
