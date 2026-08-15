package com.mi.abogado.domain.legalcase.service;

import com.mi.abogado.domain.legalcase.dto.CaseEventResponse;
import com.mi.abogado.domain.legalcase.dto.CreateCaseEventRequest;
import com.mi.abogado.domain.legalcase.entity.CaseEvent;
import com.mi.abogado.domain.legalcase.entity.CaseEventType;
import com.mi.abogado.domain.legalcase.entity.LegalCase;
import com.mi.abogado.domain.legalcase.repository.CaseEventRepository;
import com.mi.abogado.domain.legalcase.repository.LegalCaseRepository;
import com.mi.abogado.domain.user.entity.User;
import com.mi.abogado.domain.user.repository.UserRepository;
import com.mi.abogado.shared.error.BusinessException;
import com.mi.abogado.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Bitacora del expediente. Solo escribe y lee: no hay edicion ni borrado.
 */
@Service
@RequiredArgsConstructor
public class CaseEventService {

    private final CaseEventRepository caseEventRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<CaseEventResponse> findByCase(UUID caseId, Pageable pageable) {
        return caseEventRepository.findByCaseId(caseId, pageable);
    }

    /** Linea de tiempo del portal: solo lo que la firma decidio publicar. */
    @Transactional(readOnly = true)
    public List<CaseEventResponse> findSharedByCase(UUID caseId) {
        return caseEventRepository.findSharedByCaseId(caseId);
    }

    @Transactional
    public void addManualEvent(UUID caseId, CreateCaseEventRequest request) {
        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> BusinessException.notFound("Expediente"));

        caseEventRepository.save(new CaseEvent(
                legalCase, currentUser(), request.eventType(),
                request.title(), request.description(), request.occurredAt(),
                Boolean.TRUE.equals(request.visibleToClient())));
    }

    /**
     * Publica o retira del portal una entrada ya registrada.
     */
    @Transactional
    public void changeVisibility(UUID eventId, boolean visibleToClient) {
        caseEventRepository.findById(eventId)
                .orElseThrow(() -> BusinessException.notFound("Actuacion"))
                .changeVisibility(visibleToClient);
    }

    /**
     * Registro automatico desde otros servicios (apertura, cambio de estado,
     * cierre). No valida nada: quien llama ya tiene el expediente en la mano.
     * <p>
     * Nace interno. Lo que el cliente ve lo decide la firma, no un efecto colateral.
     */
    @Transactional
    public void record(LegalCase legalCase, CaseEventType eventType, String title, String description) {
        caseEventRepository.save(
                new CaseEvent(legalCase, currentUser(), eventType, title, description, null, false));
    }

    /**
     * @return el usuario de la sesion, o null si el evento lo genera un job
     *         de sistema (que no tiene sesion).
     */
    private User currentUser() {
        return CurrentUser.find()
                .flatMap(principal -> userRepository.findById(principal.userId()))
                .orElse(null);
    }
}
