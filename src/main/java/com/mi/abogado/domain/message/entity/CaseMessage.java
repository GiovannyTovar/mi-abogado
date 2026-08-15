package com.mi.abogado.domain.message.entity;

import com.mi.abogado.domain.legalcase.entity.LegalCase;
import com.mi.abogado.domain.user.entity.User;
import com.mi.abogado.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Mensaje del hilo de un expediente. Un solo hilo por caso: no hace falta una
 * entidad conversacion para modelar algo que siempre tiene dos lados conocidos
 * (la firma y su cliente).
 * <p>
 * Inmutable salvo {@code readAt}: en un canal con valor probatorio, poder editar
 * lo dicho lo invalida.
 */
@Entity
@Table(name = "case_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseMessage extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_case_id", updatable = false)
    private LegalCase legalCase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", updatable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "text", updatable = false)
    private String body;

    @Column(name = "read_at")
    private Instant readAt;

    public CaseMessage(LegalCase legalCase, User sender, String body) {
        this.legalCase = legalCase;
        this.sender = sender;
        this.body = body;
    }

    public void markRead(Instant when) {
        if (readAt == null) {
            this.readAt = when;
        }
    }
}
