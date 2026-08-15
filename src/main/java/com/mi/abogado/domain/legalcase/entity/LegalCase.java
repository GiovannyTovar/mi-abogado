package com.mi.abogado.domain.legalcase.entity;

import com.mi.abogado.domain.client.entity.Client;
import com.mi.abogado.domain.lawyer.entity.Lawyer;
import com.mi.abogado.domain.lawyer.entity.PracticeArea;
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

import java.math.BigDecimal;
import java.time.Instant;

/**
 * El expediente. Se llama {@code LegalCase} y no {@code Case} porque {@code case}
 * es palabra reservada en Java (y en SQL): ni la clase ni el paquete podrian
 * llamarse asi.
 */
@Entity
@Table(name = "legal_case")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalCase extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    /** Puede quedar sin asignar mientras el dueno decide quien lo lleva. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_lawyer_id")
    private Lawyer assignedLawyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_area_id")
    private PracticeArea practiceArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    /** Consecutivo interno de la firma, formato AAAA-NNNN. Lo asigna el sistema. */
    @Column(name = "case_number", nullable = false, length = 20, updatable = false)
    private String caseNumber;

    /** Numero de radicacion del juzgado. Solo litigio, y solo cuando ya se radico. */
    @Column(length = 30)
    private String radicado;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false, length = 20)
    private CaseType caseType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status = CaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CaseOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CasePriority priority = CasePriority.MEDIUM;

    @Column(length = 180)
    private String court;

    @Column(name = "opposing_party", length = 180)
    private String opposingParty;

    /** Cuantia de las pretensiones. */
    @Column(name = "claim_amount", precision = 14, scale = 2)
    private BigDecimal claimAmount;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    public LegalCase(Client client, String caseNumber, String title, CaseType caseType) {
        this.client = client;
        this.caseNumber = caseNumber;
        this.title = title;
        this.caseType = caseType;
    }

    public boolean isClosed() {
        return status == CaseStatus.CLOSED;
    }

    /**
     * Cerrar exige desenlace: un caso cerrado "sin resultado" no dice nada, y la
     * restriccion ck_case_closed lo rechazaria en la BD de todos modos.
     */
    public void close(CaseOutcome outcome, Instant when) {
        this.status = CaseStatus.CLOSED;
        this.outcome = outcome;
        this.closedAt = when;
    }

    /** Reabrir limpia el desenlace: vuelve a estar en curso. */
    public void reopen() {
        this.status = CaseStatus.IN_PROGRESS;
        this.outcome = null;
        this.closedAt = null;
    }
}
