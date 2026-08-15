package com.mi.abogado.domain.portal.controller;

import com.mi.abogado.domain.appointment.dto.AppointmentResponse;
import com.mi.abogado.domain.document.controller.DocumentDownloads;
import com.mi.abogado.domain.document.dto.DocumentResponse;
import com.mi.abogado.domain.message.dto.MessageResponse;
import com.mi.abogado.domain.message.dto.SendMessageRequest;
import com.mi.abogado.domain.portal.dto.PortalCaseDetail;
import com.mi.abogado.domain.portal.dto.PortalCaseSummary;
import com.mi.abogado.domain.portal.service.ClientPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * API del portal del cliente. Todo cuelga de {@code /portal} y exige rol CLIENT:
 * un abogado no entra por aqui, y un cliente no entra por los endpoints de la firma.
 * <p>
 * Ninguna ruta lleva el id del cliente: sale siempre del token. Lo que no viaja
 * por la URL no se puede manipular.
 */
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientPortalController {

    private final ClientPortalService portalService;

    // --- Casos ---

    @GetMapping("/cases")
    public List<PortalCaseSummary> myCases() {
        return portalService.myCases();
    }

    @GetMapping("/cases/{caseId}")
    public PortalCaseDetail caseDetail(@PathVariable UUID caseId) {
        return portalService.caseDetail(caseId);
    }

    // --- Documentos ---

    @GetMapping("/cases/{caseId}/documents")
    public List<DocumentResponse> documents(@PathVariable UUID caseId) {
        return portalService.caseDocuments(caseId);
    }

    @PostMapping(value = "/cases/{caseId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentResponse upload(@PathVariable UUID caseId,
                                   @RequestPart("file") MultipartFile file,
                                   @RequestParam(required = false) String description) {
        return portalService.upload(caseId, file, description);
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        return DocumentDownloads.stream(portalService.download(id));
    }

    // --- Mensajeria ---

    @GetMapping("/cases/{caseId}/messages")
    public Page<MessageResponse> messages(@PathVariable UUID caseId,
                                          @PageableDefault(size = 30) Pageable pageable) {
        return portalService.messages(caseId, pageable);
    }

    @PostMapping("/cases/{caseId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@PathVariable UUID caseId,
                                       @Valid @RequestBody SendMessageRequest request) {
        return portalService.sendMessage(caseId, request.body());
    }

    @PostMapping("/cases/{caseId}/messages/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID caseId) {
        portalService.markMessagesRead(caseId);
    }

    // --- Citas ---

    @GetMapping("/appointments")
    public List<AppointmentResponse> appointments() {
        return portalService.myAppointments();
    }

    @PostMapping("/appointments/{id}/confirm")
    public AppointmentResponse confirm(@PathVariable UUID id) {
        return portalService.confirmAppointment(id);
    }

    @PostMapping("/appointments/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable UUID id, @RequestParam(required = false) String reason) {
        return portalService.cancelAppointment(id, reason);
    }
}
