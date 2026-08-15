package com.mi.abogado.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

/**
 * Base de toda entidad que pertenece a una firma.
 * <p>
 * {@link TenantId} hace que Hibernate:
 * <ul>
 *   <li>anada {@code AND tenant_id = ?} a <b>toda</b> consulta y {@code find()} de la entidad;</li>
 *   <li>rellene {@code tenant_id} en el insert desde el tenant del contexto.</li>
 * </ul>
 * Es decir: el aislamiento no depende de que el desarrollador se acuerde de filtrar.
 * El valor lo aporta {@code TenantIdentifierResolver} leyendo el {@code TenantContext}.
 * <p>
 * Ojo: {@code Tenant} y {@code User} <b>no</b> heredan de aqui. Ambas se consultan
 * durante el login, cuando todavia no hay tenant resuelto; su aislamiento se hace
 * de forma explicita en el service.
 */
@Getter
@MappedSuperclass
public abstract class TenantScopedEntity extends BaseEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
}
