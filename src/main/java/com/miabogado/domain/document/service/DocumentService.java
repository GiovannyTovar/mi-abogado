package com.miabogado.domain.document.service;

import com.miabogado.domain.document.dto.DocumentContent;
import com.miabogado.domain.document.dto.DocumentResponse;
import com.miabogado.domain.document.entity.Document;
import com.miabogado.domain.document.entity.DocumentSource;
import com.miabogado.domain.document.entity.DocumentVisibility;
import com.miabogado.domain.document.mapper.DocumentMapper;
import com.miabogado.domain.document.repository.DocumentRepository;
import com.miabogado.domain.legalcase.entity.CaseEventType;
import com.miabogado.domain.legalcase.entity.LegalCase;
import com.miabogado.domain.legalcase.repository.LegalCaseRepository;
import com.miabogado.domain.legalcase.service.CaseEventService;
import com.miabogado.domain.user.entity.User;
import com.miabogado.domain.user.repository.UserRepository;
import com.miabogado.shared.error.BusinessException;
import com.miabogado.shared.security.CurrentUser;
import com.miabogado.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Archivos del expediente.
 * <p>
 * El modulo es autocontenido a proposito: fuera de aqui nadie sabe donde ni como
 * se guardan los binarios. Cuando en v2 entre la IA (extraer texto, resumir,
 * generar borradores), el trabajo cae dentro de este servicio y del almacen —
 * ningun otro dominio se entera.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final UserRepository userRepository;
    private final DocumentStorage documentStorage;
    private final CaseEventService caseEventService;
    private final DocumentMapper documentMapper;

    @Transactional(readOnly = true)
    public List<DocumentResponse> findByCase(UUID caseId) {
        return documentMapper.toResponses(documentRepository.findByLegalCase_IdOrderByCreatedAtDesc(caseId));
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> findSharedByCase(UUID caseId) {
        return documentMapper.toResponses(documentRepository.findSharedByCaseId(caseId));
    }

    @Transactional
    public DocumentResponse upload(UUID caseId, MultipartFile file, String description, DocumentSource source) {
        if (file.isEmpty()) {
            throw BusinessException.conflict("El archivo esta vacio");
        }

        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> BusinessException.notFound("Expediente"));

        String storageKey = documentStorage.store(file, TenantContext.require(), caseId);

        Document document = new Document(
                legalCase,
                currentUser(),
                sanitizeName(file.getOriginalFilename()),
                storageKey,
                file.getContentType(),
                file.getSize(),
                source);
        document.setDescription(description);
        documentRepository.save(document);

        caseEventService.record(legalCase, CaseEventType.DOCUMENTO,
                "Documento agregado: " + document.getName(), description);

        return documentMapper.toResponse(document);
    }

    /**
     * Descarga para la firma. La del portal pasa por {@code ClientPortalService},
     * que ademas comprueba que el caso sea del cliente y el archivo este compartido.
     */
    @Transactional(readOnly = true)
    public DocumentContent download(UUID id) {
        Document document = requireDocument(id);
        return new DocumentContent(
                documentStorage.load(document.getStorageKey()),
                document.getName(),
                document.getContentType());
    }

    @Transactional
    public DocumentResponse changeVisibility(UUID id, DocumentVisibility visibility) {
        Document document = requireDocument(id);

        if (document.getSource() == DocumentSource.CLIENT
                && visibility == DocumentVisibility.INTERNAL) {
            throw BusinessException.conflict("Un documento que subio el cliente no se le puede ocultar");
        }

        document.setVisibility(visibility);
        return documentMapper.toResponse(document);
    }

    @Transactional
    public void delete(UUID id) {
        Document document = requireDocument(id);
        String storageKey = document.getStorageKey();

        documentRepository.delete(document);
        caseEventService.record(document.getLegalCase(), CaseEventType.DOCUMENTO,
                "Documento eliminado: " + document.getName(), null);

        // Ultimo paso: si el borrado en BD falla, el archivo sigue ahi.
        documentStorage.delete(storageKey);
    }

    /** Para el portal: recuperar el documento y decidir si el cliente puede verlo. */
    @Transactional(readOnly = true)
    public Document requireDocument(UUID id) {
        return documentRepository.findWithDetailsById(id)
                .orElseThrow(() -> BusinessException.notFound("Documento"));
    }

    public DocumentContent contentOf(Document document) {
        return new DocumentContent(
                documentStorage.load(document.getStorageKey()),
                document.getName(),
                document.getContentType());
    }

    /** El nombre solo se muestra, pero no se guarda con separadores de ruta. */
    private String sanitizeName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "documento";
        }
        String cleaned = originalName.replaceAll("[/\\\\]", "_").trim();
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    private User currentUser() {
        return CurrentUser.find()
                .flatMap(principal -> userRepository.findById(principal.userId()))
                .orElse(null);
    }
}
