package com.miabogado.domain.appointment.entity;

import com.miabogado.domain.client.entity.Client;
import com.miabogado.domain.lawyer.entity.Lawyer;
import com.miabogado.domain.legalcase.entity.LegalCase;
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

/**
 * Cita con un cliente. El expediente es opcional: la primera cita suele ocurrir
 * antes de que exista el caso, y exigirlo obligaria a abrir expedientes vacios.
 */
@Entity
@Table(name = "appointment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_case_id")
    private LegalCase legalCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_id")
    private Lawyer lawyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentMode mode = AppointmentMode.PRESENCIAL;

    /** Direccion si es presencial, enlace si es virtual. */
    @Column(length = 250)
    private String location;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(name = "cancel_reason", length = 250)
    private String cancelReason;

    public Appointment(Client client, String title, Instant startsAt, Instant endsAt) {
        this.client = client;
        this.title = title;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public boolean isOpen() {
        return status == AppointmentStatus.SCHEDULED || status == AppointmentStatus.CONFIRMED;
    }

    public void confirm() {
        this.status = AppointmentStatus.CONFIRMED;
    }

    public void cancel(String reason) {
        this.status = AppointmentStatus.CANCELLED;
        this.cancelReason = reason;
    }

    public void complete() {
        this.status = AppointmentStatus.COMPLETED;
    }
}
