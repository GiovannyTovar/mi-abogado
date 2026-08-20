package com.miabogado.domain.lead.service;

import com.miabogado.domain.client.dto.CreateClientRequest;
import com.miabogado.domain.client.entity.Client;
import com.miabogado.domain.client.service.ClientService;
import com.miabogado.domain.lawyer.entity.Lawyer;
import com.miabogado.domain.lawyer.entity.PracticeArea;
import com.miabogado.domain.lawyer.repository.LawyerRepository;
import com.miabogado.domain.lawyer.repository.PracticeAreaRepository;
import com.miabogado.domain.lead.dto.ConvertLeadRequest;
import com.miabogado.domain.lead.dto.CreateLeadRequest;
import com.miabogado.domain.lead.dto.LeadPipelineStage;
import com.miabogado.domain.lead.dto.LeadResponse;
import com.miabogado.domain.lead.dto.LeadSummary;
import com.miabogado.domain.lead.dto.UpdateLeadRequest;
import com.miabogado.domain.lead.entity.Lead;
import com.miabogado.domain.lead.entity.LeadSource;
import com.miabogado.domain.lead.entity.LeadStatus;
import com.miabogado.domain.lead.mapper.LeadMapper;
import com.miabogado.domain.lead.repository.LeadRepository;
import com.miabogado.domain.legalcase.dto.CreateCaseRequest;
import com.miabogado.domain.legalcase.entity.CaseType;
import com.miabogado.domain.legalcase.service.LegalCaseService;
import com.miabogado.shared.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Pipeline de captacion: del primer contacto hasta que se vuelve cliente.
 */
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final LawyerRepository lawyerRepository;
    private final PracticeAreaRepository practiceAreaRepository;
    private final ClientService clientService;
    private final LegalCaseService legalCaseService;
    private final LeadMapper leadMapper;

    @Transactional(readOnly = true)
    public Page<LeadSummary> search(LeadStatus status, LeadSource source, UUID lawyerId, Pageable pageable) {
        return leadRepository.search(status, source, lawyerId, pageable);
    }

    @Transactional(readOnly = true)
    public List<LeadPipelineStage> pipeline() {
        return leadRepository.countByStage();
    }

    @Transactional(readOnly = true)
    public LeadResponse findById(UUID id) {
        return leadMapper.toResponse(requireLead(id));
    }

    @Transactional
    public LeadResponse create(CreateLeadRequest request) {
        requireSomeContact(request.email(), request.phone());

        Lead lead = new Lead(request.name(), request.source());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setCity(request.city());
        lead.setMessage(request.message());
        lead.setPracticeArea(resolvePracticeArea(request.practiceAreaId()));
        lead.setAssignedLawyer(resolveLawyer(request.assignedLawyerId()));

        return leadMapper.toResponse(leadRepository.save(lead));
    }

    @Transactional
    public LeadResponse update(UUID id, UpdateLeadRequest request) {
        Lead lead = requireLead(id);

        if (request.status() == LeadStatus.CONVERTED) {
            throw BusinessException.conflict("Para convertir un lead usa /leads/{id}/convert: crea el cliente");
        }
        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw BusinessException.conflict("El lead ya se convirtio en cliente");
        }

        applyChanges(lead, request);
        requireSomeContact(lead.getEmail(), lead.getPhone());
        return leadMapper.toResponse(lead);
    }

    @Transactional
    public LeadResponse markContacted(UUID id) {
        Lead lead = requireLead(id);
        if (lead.isClosed()) {
            throw BusinessException.conflict("El lead ya esta cerrado");
        }
        lead.markContacted(Instant.now());
        return leadMapper.toResponse(lead);
    }

    /**
     * Convierte el lead en cliente y, si se indica titulo, le abre su primer
     * expediente. Todo en una transaccion: o queda cliente con caso, o nada.
     */
    @Transactional
    public LeadResponse convert(UUID id, ConvertLeadRequest request) {
        Lead lead = requireLead(id);
        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw BusinessException.conflict("El lead ya se convirtio en cliente");
        }

        // Los datos de contacto ya los trae el lead; solo falta el documento.
        Client client = clientService.createClient(new CreateClientRequest(
                request.clientType(),
                request.documentType(),
                request.documentNumber(),
                lead.getName(),
                lead.getEmail(),
                lead.getPhone(),
                null,
                lead.getCity(),
                lead.getMessage()));

        lead.convert(client);

        if (request.openCaseTitle() != null && !request.openCaseTitle().isBlank()) {
            legalCaseService.create(new CreateCaseRequest(
                    client.getId(),
                    request.openCaseTitle(),
                    CaseType.LITIGIO,
                    lead.getMessage(),
                    lead.getAssignedLawyer() == null ? null : lead.getAssignedLawyer().getId(),
                    lead.getPracticeArea() == null ? null : lead.getPracticeArea().getId(),
                    null, null, null, null, null));
        }

        return leadMapper.toResponse(lead);
    }

    @Transactional
    public LeadResponse markLost(UUID id, String reason) {
        Lead lead = requireLead(id);
        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw BusinessException.conflict("Un lead convertido no puede marcarse como perdido");
        }
        lead.markLost(reason);
        return leadMapper.toResponse(lead);
    }

    private void applyChanges(Lead lead, UpdateLeadRequest request) {
        if (request.name() != null) {
            lead.setName(request.name());
        }
        if (request.email() != null) {
            lead.setEmail(request.email());
        }
        if (request.phone() != null) {
            lead.setPhone(request.phone());
        }
        if (request.city() != null) {
            lead.setCity(request.city());
        }
        if (request.message() != null) {
            lead.setMessage(request.message());
        }
        if (request.status() != null) {
            lead.setStatus(request.status());
        }
        if (request.lostReason() != null) {
            lead.setLostReason(request.lostReason());
        }
        if (request.practiceAreaId() != null) {
            lead.setPracticeArea(resolvePracticeArea(request.practiceAreaId()));
        }
        if (request.assignedLawyerId() != null) {
            lead.setAssignedLawyer(resolveLawyer(request.assignedLawyerId()));
        }
    }

    private void requireSomeContact(String email, String phone) {
        boolean noEmail = email == null || email.isBlank();
        boolean noPhone = phone == null || phone.isBlank();
        if (noEmail && noPhone) {
            throw BusinessException.conflict("El lead necesita al menos correo o telefono");
        }
    }

    private Lead requireLead(UUID id) {
        return leadRepository.findWithDetailsById(id)
                .orElseThrow(() -> BusinessException.notFound("Lead"));
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
