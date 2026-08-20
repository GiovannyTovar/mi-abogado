package com.miabogado.domain.lead.entity;

import com.miabogado.domain.client.entity.Client;
import com.miabogado.domain.lawyer.entity.Lawyer;
import com.miabogado.domain.lawyer.entity.PracticeArea;
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
 * Contacto que todavia no es cliente. Es la mitad del valor del producto: el
 * directorio publico y la calculadora existen para llenar esta tabla.
 */
@Entity
@Table(name = "lead")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lead extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_area_id")
    private PracticeArea practiceArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_lawyer_id")
    private Lawyer assignedLawyer;

    /** Se llena al convertir; antes es null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_client_id")
    private Client convertedClient;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 180)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadSource source;

    /** La consulta con la que llego. */
    @Column(columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadStatus status = LeadStatus.NEW;

    @Column(name = "lost_reason", length = 250)
    private String lostReason;

    @Column(name = "contacted_at")
    private Instant contactedAt;

    public Lead(String name, LeadSource source) {
        this.name = name;
        this.source = source;
    }

    public boolean isClosed() {
        return status == LeadStatus.CONVERTED || status == LeadStatus.LOST;
    }

    public void markContacted(Instant when) {
        if (status == LeadStatus.NEW) {
            this.status = LeadStatus.CONTACTED;
        }
        this.contactedAt = when;
    }

    public void convert(Client client) {
        this.status = LeadStatus.CONVERTED;
        this.convertedClient = client;
        this.lostReason = null;
    }

    public void markLost(String reason) {
        this.status = LeadStatus.LOST;
        this.lostReason = reason;
    }
}
