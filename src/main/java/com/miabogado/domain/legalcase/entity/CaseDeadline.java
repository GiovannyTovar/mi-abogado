package com.miabogado.domain.legalcase.entity;

import com.miabogado.domain.user.entity.User;
import com.miabogado.shared.persistence.TenantScopedEntity;
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

import java.time.Instant;
import java.time.LocalDate;

/**
 * Termino procesal o vencimiento de un expediente.
 * <p>
 * {@code dueDate} es {@link LocalDate}, no un instante: los terminos judiciales
 * vencen "el 15 de marzo", no "el 15 de marzo a las 17:00 UTC". Guardarlo con
 * hora invita a errores de zona horaria justo donde mas caro sale.
 */
@Entity
@Table(name = "case_deadline")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseDeadline extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_case_id", updatable = false)
    private LegalCase legalCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private User completedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "deadline_type", nullable = false, length = 20)
    private DeadlineType deadlineType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** Dias de antelacion con que debe avisarse. La Fase 7 lo usara para WhatsApp. */
    @Column(name = "notify_days_before", nullable = false)
    private short notifyDaysBefore = 3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeadlineStatus status = DeadlineStatus.PENDING;

    @Column(name = "completed_at")
    private Instant completedAt;

    public CaseDeadline(LegalCase legalCase, User createdBy, DeadlineType deadlineType,
                        String title, LocalDate dueDate) {
        this.legalCase = legalCase;
        this.createdBy = createdBy;
        this.deadlineType = deadlineType;
        this.title = title;
        this.dueDate = dueDate;
    }

    public boolean isPending() {
        return status == DeadlineStatus.PENDING;
    }

    public void complete(User completedBy, Instant when) {
        this.status = DeadlineStatus.COMPLETED;
        this.completedBy = completedBy;
        this.completedAt = when;
    }

    public void markMissed() {
        this.status = DeadlineStatus.MISSED;
    }
}
