package com.mi.abogado.domain.portal.service;

import com.mi.abogado.domain.appointment.dto.AppointmentResponse;
import com.mi.abogado.domain.appointment.entity.Appointment;
import com.mi.abogado.domain.appointment.service.AppointmentService;
import com.mi.abogado.domain.client.entity.Client;
import com.mi.abogado.domain.client.repository.ClientRepository;
import com.mi.abogado.domain.document.dto.DocumentContent;
import com.mi.abogado.domain.document.dto.DocumentResponse;
import com.mi.abogado.domain.document.entity.Document;
import com.mi.abogado.domain.document.entity.DocumentSource;
import com.mi.abogado.domain.document.service.DocumentService;
import com.mi.abogado.domain.legalcase.entity.LegalCase;
import com.mi.abogado.domain.legalcase.service.CaseEventService;
import com.mi.abogado.domain.message.dto.MessageResponse;
import com.mi.abogado.domain.message.service.CaseMessageService;
import com.mi.abogado.domain.portal.dto.PortalCaseDetail;
import com.mi.abogado.domain.portal.dto.PortalCaseSummary;
import com.mi.abogado.domain.portal.repository.PortalCaseRepository;
import com.mi.abogado.shared.error.BusinessException;
import com.mi.abogado.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Portal del cliente final.
 * <p>
 * <b>Por que es un modulo aparte y no un rol mas en los endpoints de la firma:</b>
 * el filtro de tenant de Hibernate aisla una firma de otra, pero <i>no</i> aisla a
 * un cliente de los demas clientes de su misma firma. Ese segundo aislamiento hay
 * que escribirlo, y concentrarlo en un unico servicio con una unica puerta de
 * entrada ({@link #requireOwnCase}) es mucho mas facil de auditar que repartir
 * {@code if (esCliente)} por los servicios de la firma.
 * <p>
 * Regla invariable: <b>toda</b> operacion parte de {@link #currentClient()} y pasa
 * por {@link #requireOwnCase}. Un caso que no sea de ese cliente responde 404, no
 * 403: confirmar que existe ya seria filtrar informacion.
 */
@Service
@RequiredArgsConstructor
public class ClientPortalService {

    private final ClientRepository clientRepository;
    private final PortalCaseRepository portalCaseRepository;
    private final DocumentService documentService;
    private final CaseEventService caseEventService;
    private final CaseMessageService caseMessageService;
    private final AppointmentService appointmentService;

    // --- Casos ---

    @Transactional(readOnly = true)
    public List<PortalCaseSummary> myCases() {
        return portalCaseRepository.findCasesOf(currentClient().getId(), CurrentUser.require().userId());
    }

    @Transactional(readOnly = true)
    public PortalCaseDetail caseDetail(UUID caseId) {
        LegalCase legalCase = requireOwnCase(caseId);

        return new PortalCaseDetail(
                legalCase.getId(),
                legalCase.getCaseNumber(),
                legalCase.getTitle(),
                legalCase.getDescription(),
                legalCase.getCaseType(),
                legalCase.getStatus(),
                legalCase.getOutcome(),
                legalCase.getAssignedLawyer() == null ? null : legalCase.getAssignedLawyer().getUser().getFullName(),
                legalCase.getPracticeArea() == null ? null : legalCase.getPracticeArea().getName(),
                legalCase.getOpenedAt(),
                legalCase.getClosedAt(),
                caseEventService.findSharedByCase(caseId),
                documentService.findSharedByCase(caseId));
    }

    // --- Documentos ---

    @Transactional(readOnly = true)
    public List<DocumentResponse> caseDocuments(UUID caseId) {
        requireOwnCase(caseId);
        return documentService.findSharedByCase(caseId);
    }

    @Transactional
    public DocumentResponse upload(UUID caseId, MultipartFile file, String description) {
        LegalCase legalCase = requireOwnCase(caseId);
        if (legalCase.isClosed()) {
            throw BusinessException.conflict("El expediente esta cerrado");
        }
        return documentService.upload(caseId, file, description, DocumentSource.CLIENT);
    }

    /**
     * Descarga con doble comprobacion: que el documento sea de un caso suyo y que
     * este compartido. Con solo lo primero, un id filtrado dejaria ver documentos
     * internos de su propio caso.
     */
    @Transactional(readOnly = true)
    public DocumentContent download(UUID documentId) {
        Document document = documentService.requireDocument(documentId);
        requireOwnCase(document.getLegalCase().getId());

        if (!document.isSharedWithClient()) {
            throw BusinessException.notFound("Documento");
        }
        return documentService.contentOf(document);
    }

    // --- Mensajeria ---

    @Transactional(readOnly = true)
    public Page<MessageResponse> messages(UUID caseId, Pageable pageable) {
        requireOwnCase(caseId);
        return caseMessageService.findThread(caseId, pageable);
    }

    @Transactional
    public MessageResponse sendMessage(UUID caseId, String body) {
        requireOwnCase(caseId);
        return caseMessageService.send(caseId, body);
    }

    @Transactional
    public void markMessagesRead(UUID caseId) {
        requireOwnCase(caseId);
        caseMessageService.markRead(caseId);
    }

    // --- Citas ---

    @Transactional(readOnly = true)
    public List<AppointmentResponse> myAppointments() {
        return appointmentService.upcomingForClient(currentClient().getId());
    }

    @Transactional
    public AppointmentResponse confirmAppointment(UUID appointmentId) {
        requireOwnAppointment(appointmentId);
        return appointmentService.confirm(appointmentId);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(UUID appointmentId, String reason) {
        requireOwnAppointment(appointmentId);
        return appointmentService.cancel(appointmentId, reason);
    }

    // --- Puertas de entrada ---

    /**
     * El cliente detras de la sesion. Si el usuario tiene rol CLIENT pero su ficha
     * fue desvinculada, no hay portal que mostrar.
     */
    private Client currentClient() {
        return clientRepository.findByUser_Id(CurrentUser.require().userId())
                .orElseThrow(() -> BusinessException.forbidden("Esta cuenta no tiene un cliente asociado"));
    }

    /**
     * @throws BusinessException 404 si el caso no existe <i>o</i> no es de este
     *         cliente. La misma respuesta en ambos casos, a proposito.
     */
    private LegalCase requireOwnCase(UUID caseId) {
        return portalCaseRepository.findByIdAndClient_Id(caseId, currentClient().getId())
                .orElseThrow(() -> BusinessException.notFound("Expediente"));
    }

    private void requireOwnAppointment(UUID appointmentId) {
        Appointment appointment = appointmentService.requireAppointment(appointmentId);
        if (!appointment.getClient().getId().equals(currentClient().getId())) {
            throw BusinessException.notFound("Cita");
        }
    }
}
