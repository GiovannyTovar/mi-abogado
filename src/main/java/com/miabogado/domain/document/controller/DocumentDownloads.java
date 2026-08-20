package com.miabogado.domain.document.controller;

import com.miabogado.domain.document.dto.DocumentContent;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/**
 * Respuesta de descarga, compartida por el controller de la firma y el del portal.
 */
public final class DocumentDownloads {

    private DocumentDownloads() {
    }

    /**
     * {@code attachment} y no {@code inline}: un HTML o un SVG subido por un cliente
     * se ejecutaria con el dominio de la aplicacion si el navegador lo renderizara.
     * {@code nosniff} cierra la misma puerta por el lado del tipo adivinado.
     */
    public static ResponseEntity<Resource> stream(DocumentContent content) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(content.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CONTENT_TYPE, content.contentType())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.resource());
    }
}
