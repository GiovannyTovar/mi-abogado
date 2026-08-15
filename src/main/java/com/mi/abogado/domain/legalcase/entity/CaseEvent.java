package com.mi.abogado.domain.legalcase.entity;

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

import java.time.Instant;

/**
 * Entrada de la bitacora del expediente. Solo se agrega: no hay update ni delete.
 * Es el historial de lo que paso, y reescribirlo destruiria su valor probatorio
 * frente al cliente.
 */
@Entity
@Table(name = "case_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseEvent extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_case_id", updatable = false)
    private LegalCase legalCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20, updatable = false)
    private CaseEventType eventType;

    @Column(nullable = false, length = 200, updatable = false)
    private String title;

    @Column(columnDefinition = "text", updatable = false)
    private String description;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    public CaseEvent(LegalCase legalCase, User createdBy, CaseEventType eventType,
                     String title, String description, Instant occurredAt) {
        this.legalCase = legalCase;
        this.createdBy = createdBy;
        this.eventType = eventType;
        this.title = title;
        this.description = description;
        if (occurredAt != null) {
            this.occurredAt = occurredAt;
        }
    }
}
