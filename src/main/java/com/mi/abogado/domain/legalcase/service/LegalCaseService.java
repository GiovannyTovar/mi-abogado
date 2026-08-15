package com.mi.abogado.domain.legalcase.service;

import com.mi.abogado.domain.client.service.ClientService;
import com.mi.abogado.domain.lawyer.entity.Lawyer;
import com.mi.abogado.domain.lawyer.entity.PracticeArea;
import com.mi.abogado.domain.lawyer.repository.LawyerRepository;
import com.mi.abogado.domain.lawyer.repository.PracticeAreaRepository;
import com.mi.abogado.domain.legalcase.dto.CaseResponse;
import com.mi.abogado.domain.legalcase.dto.CaseSummary;
import com.mi.abogado.domain.legalcase.dto.CloseCaseRequest;
import com.mi.abogado.domain.legalcase.dto.CreateCaseRequest;
import com.mi.abogado.domain.legalcase.dto.UpdateCaseRequest;
import com.mi.abogado.domain.legalcase.entity.CaseEventType;
import com.mi.abogado.domain.legalcase.entity.CaseStatus;
import com.mi.abogado.domain.legalcase.entity.CaseType;
import com.mi.abogado.domain.legalcase.entity.LegalCase;
import com.mi.abogado.domain.legalcase.mapper.LegalCaseMapper;
import com.mi.abogado.domain.legalcase.repository.LegalCaseRepository;
import com.mi.abogado.shared.error.BusinessException;
import com.mi.abogado.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Casos de uso del expediente.
 * <p>
 * Cada cambio relevante deja rastro en la bitacora ({@code CaseEvent}): el cliente
 * tiene derecho a saber que paso con su caso y cuando, y en la Fase 4 esa bitacora
 * es justo lo que vera en su portal.
 */
@Service
@RequiredArgsConstructor
public class LegalCaseService {

    private final LegalCaseRepository legalCaseRepository;
    private final LawyerRepository lawyerRepository;
    private final PracticeAreaRepository practiceAreaRepository;
    private final ClientService clientService;
    private final CaseNumberGenerator caseNumberGenerator;
    private final CaseEventService caseEventService;
    private final LegalCaseMapper legalCaseMapper;

    @Transactional(readOnly = true)
    public Page<CaseSummary> search(CaseStatus status, CaseType caseType, UUID clientId,
                                    UUID lawyerId, String search, Pageable pageable) {
        return legalCaseRepository.search(status, caseType, clientId, lawyerId, search, pageable);
    }

    @Transactional(readOnly = true)
    public CaseResponse findById(UUID id) {
        return legalCaseMapper.toResponse(requireCaseWithDetails(id));
    }

    @Transactional
    public CaseResponse create(CreateCaseRequest request) {
        if (request.radicado() != null && legalCaseRepository.existsByRadicado(request.radicado())) {
            throw BusinessException.conflict("Ya existe un expediente con ese radicado");
        }

        LegalCase legalCase = new LegalCase(
                clientService.requireClient(request.clientId()),
                caseNumberGenerator.next(TenantContext.require()),
                request.title(),
                request.caseType());

        legalCase.setDescription(request.description());
        legalCase.setRadicado(request.radicado());
        legalCase.setCourt(request.court());
        legalCase.setOpposingParty(request.opposingParty());
        legalCase.setClaimAmount(request.claimAmount());
        legalCase.setAssignedLawyer(resolveLawyer(request.assignedLawyerId()));
        legalCase.setPracticeArea(resolvePracticeArea(request.practiceAreaId()));
        if (request.priority() != null) {
            legalCase.setPriority(request.priority());
        }

        legalCaseRepository.save(legalCase);
        caseEventService.record(legalCase, CaseEventType.CAMBIO_ESTADO,
                "Expediente abierto", "Numero interno " + legalCase.getCaseNumber());

        return legalCaseMapper.toResponse(legalCase);
    }

    @Transactional
    public CaseResponse update(UUID id, UpdateCaseRequest request) {
        LegalCase legalCase = requireCaseWithDetails(id);

        if (request.status() == CaseStatus.CLOSED) {
            throw BusinessException.conflict("Para cerrar un expediente usa /cases/{id}/close: hace falta el desenlace");
        }
        if (legalCase.isClosed()) {
            throw BusinessException.conflict("El expediente esta cerrado. Reabrelo antes de editarlo.");
        }
        if (request.radicado() != null
                && !request.radicado().equals(legalCase.getRadicado())
                && legalCaseRepository.existsByRadicado(request.radicado())) {
            throw BusinessException.conflict("Ya existe un expediente con ese radicado");
        }

        applyChanges(legalCase, request);
        return legalCaseMapper.toResponse(legalCase);
    }

    @Transactional
    public CaseResponse close(UUID id, CloseCaseRequest request) {
        LegalCase legalCase = requireCaseWithDetails(id);
        if (legalCase.isClosed()) {
            throw BusinessException.conflict("El expediente ya esta cerrado");
        }

        legalCase.close(request.outcome(), Instant.now());
        caseEventService.record(legalCase, CaseEventType.CAMBIO_ESTADO,
                "Expediente cerrado: " + request.outcome(), request.closingNote());

        return legalCaseMapper.toResponse(legalCase);
    }

    @Transactional
    public CaseResponse reopen(UUID id) {
        LegalCase legalCase = requireCaseWithDetails(id);
        if (!legalCase.isClosed()) {
            throw BusinessException.conflict("El expediente no esta cerrado");
        }

        legalCase.reopen();
        caseEventService.record(legalCase, CaseEventType.CAMBIO_ESTADO, "Expediente reabierto", null);

        return legalCaseMapper.toResponse(legalCase);
    }

    /** Para otros dominios que necesitan la entidad (bitacora, terminos). */
    @Transactional(readOnly = true)
    public LegalCase requireCase(UUID id) {
        return legalCaseRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Expediente"));
    }

    private void applyChanges(LegalCase legalCase, UpdateCaseRequest request) {
        if (request.title() != null) {
            legalCase.setTitle(request.title());
        }
        if (request.description() != null) {
            legalCase.setDescription(request.description());
        }
        if (request.status() != null && request.status() != legalCase.getStatus()) {
            CaseStatus previous = legalCase.getStatus();
            legalCase.setStatus(request.status());
            caseEventService.record(legalCase, CaseEventType.CAMBIO_ESTADO,
                    "Estado: %s -> %s".formatted(previous, request.status()), null);
        }
        if (request.priority() != null) {
            legalCase.setPriority(request.priority());
        }
        if (request.assignedLawyerId() != null) {
            Lawyer lawyer = resolveLawyer(request.assignedLawyerId());
            legalCase.setAssignedLawyer(lawyer);
            caseEventService.record(legalCase, CaseEventType.NOTA,
                    "Expediente asignado a " + lawyer.getUser().getFullName(), null);
        }
        if (request.practiceAreaId() != null) {
            legalCase.setPracticeArea(resolvePracticeArea(request.practiceAreaId()));
        }
        if (request.radicado() != null) {
            legalCase.setRadicado(request.radicado());
        }
        if (request.court() != null) {
            legalCase.setCourt(request.court());
        }
        if (request.opposingParty() != null) {
            legalCase.setOpposingParty(request.opposingParty());
        }
        if (request.claimAmount() != null) {
            legalCase.setClaimAmount(request.claimAmount());
        }
    }

    private LegalCase requireCaseWithDetails(UUID id) {
        return legalCaseRepository.findWithDetailsById(id)
                .orElseThrow(() -> BusinessException.notFound("Expediente"));
    }

    private Lawyer resolveLawyer(UUID lawyerId) {
        if (lawyerId == null) {
            return null;
        }
        return lawyerRepository.findWithDetailsById(lawyerId)
                .orElseThrow(() -> BusinessException.notFound("Abogado"));
    }

    private PracticeArea resolvePracticeArea(UUID practiceAreaId) {
        if (practiceAreaId == null) {
            return null;
        }
        return practiceAreaRepository.findById(practiceAreaId)
                .orElseThrow(() -> BusinessException.notFound("Especialidad"));
    }
}
