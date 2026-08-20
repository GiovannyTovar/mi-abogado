package com.miabogado.domain.subscription.repository;

import com.miabogado.domain.subscription.entity.Subscription;
import com.miabogado.domain.subscription.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /**
     * La suscripcion vigente de una firma. El plan viene resuelto porque casi
     * siempre se consulta para leer sus limites.
     */
    @EntityGraph(attributePaths = "plan")
    @Query("""
            select s from Subscription s
            where s.tenant.id = :tenantId and s.status <> 'CANCELLED'
            """)
    Optional<Subscription> findCurrentByTenantId(@Param("tenantId") UUID tenantId);

    /** Pruebas vencidas: el job diario las pasa a PAST_DUE. */
    @EntityGraph(attributePaths = {"plan", "tenant"})
    List<Subscription> findByStatusAndTrialEndsAtBefore(SubscriptionStatus status, Instant deadline);
}
