package com.mi.abogado.domain.document.controller;

import com.mi.abogado.domain.document.dto.DocumentResponse;
import com.mi.abogado.domain.document.entity.DocumentSource;
import com.mi.abogado.domain.document.entity.DocumentVisibility;
import com.mi.abogado.domain.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/cases/{caseId}/documents")
    public List<DocumentResponse> findByCase(@PathVariable UUID caseId) {
        return documentService.findByCase(caseId);
    }

    @PostMapping(value = "/cases/{caseId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentResponse upload(@PathVariable UUID caseId,
                                   @RequestPart("file") MultipartFile file,
                                   @RequestParam(required = false) String description) {
        return documentService.upload(caseId, file, description, DocumentSource.FIRM);
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        return DocumentDownloads.stream(documentService.download(id));
    }

    @PatchMapping("/documents/{id}/visibility")
    public DocumentResponse changeVisibility(@PathVariable UUID id, @RequestParam DocumentVisibility visibility) {
        return documentService.changeVisibility(id, visibility);
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
