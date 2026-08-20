package com.miabogado.shared.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Puente entre el {@link TenantContext} de la peticion y el filtro de Hibernate.
 * Hibernate lo consulta al abrir cada sesion para resolver el valor de {@code @TenantId}.
 */
@Component
public class TenantIdentifierResolver
        implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        // Nunca null: sin tenant devolvemos el sentinela y la consulta no ve nada.
        return TenantContext.find().orElse(TenantContext.NO_TENANT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
