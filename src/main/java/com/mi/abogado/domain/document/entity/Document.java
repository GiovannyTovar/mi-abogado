package com.mi.abogado.domain.document.entity;

import com.mi.abogado.domain.legalcase.entity.LegalCase;
import com.mi.abogado.domain.user.entity.User;
import com.mi.abogado.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadatos de un archivo del expediente. El binario no vive aqui: vive en el
 * almacen, y {@code storageKey} es la unica referencia hacia el.
 * <p>
 * <b>Frontera para la v2:</b> {@code extractedText} esta reservado para cuando se
 * conecte OCR o un LLM. Nadie fuera del modulo {@code document} lo lee, asi que
 * llenarlo mas adelante no obliga a tocar ningun otro dominio.
 */
@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_case_id", updatable = false)
    private LegalCase legalCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", updatable = false)
    private User uploadedBy;

    /** Nombre original con el que se subio. Se muestra, no se usa para leer del disco. */
    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "storage_key", nullable = false, length = 400, updatable = false)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 120, updatable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentVisibility visibility = DocumentVisibility.INTERNAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private DocumentSource source = DocumentSource.FIRM;

    @Column(length = 400)
    private String description;

    /** Reservado para v2. Hoy siempre null. */
    @Column(name = "extracted_text", columnDefinition = "text")
    private String extractedText;

    public Document(LegalCase legalCase, User uploadedBy, String name, String storageKey,
                    String contentType, long sizeBytes, DocumentSource source) {
        this.legalCase = legalCase;
        this.uploadedBy = uploadedBy;
        this.name = name;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.source = source;
        // Lo que sube el cliente ya lo conoce el cliente: ocultarselo no tendria sentido.
        this.visibility = source == DocumentSource.CLIENT
                ? DocumentVisibility.SHARED_WITH_CLIENT
                : DocumentVisibility.INTERNAL;
    }

    public boolean isSharedWithClient() {
        return visibility == DocumentVisibility.SHARED_WITH_CLIENT;
    }
}
