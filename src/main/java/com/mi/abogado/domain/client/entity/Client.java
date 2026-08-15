package com.mi.abogado.domain.client.entity;

import com.mi.abogado.domain.user.entity.User;
import com.mi.abogado.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cliente de la firma. En la Fase 4 se le enlazara un {@code User} para que
 * entre al portal; hoy es solo una ficha del CRM.
 */
@Entity
@Table(name = "client")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client extends TenantScopedEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 20)
    private ClientType clientType;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 30)
    private String documentNumber;

    /** Nombre completo si es persona natural; razon social si es juridica. */
    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 180)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 250)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(columnDefinition = "text")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientStatus status = ClientStatus.ACTIVE;

    /**
     * Usuario con el que entra al portal. Null mientras no se le haya dado acceso:
     * muchos clientes de un despacho nunca lo pediran.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Client(ClientType clientType, DocumentType documentType, String documentNumber, String name) {
        this.clientType = clientType;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.name = name;
    }

    public boolean isActive() {
        return status == ClientStatus.ACTIVE;
    }
}
