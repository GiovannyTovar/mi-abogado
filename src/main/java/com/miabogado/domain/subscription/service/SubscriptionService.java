package com.miabogado.domain.subscription.service;

import com.miabogado.domain.subscription.dto.SubscriptionPlanResponse;
import com.miabogado.domain.subscription.dto.SubscriptionResponse;
import com.miabogado.domain.subscription.entity.Subscription;
import com.miabogado.domain.subscription.entity.SubscriptionPlan;
import com.miabogado.domain.subscription.entity.SubscriptionStatus;
import com.miabogado.domain.subscription.mapper.SubscriptionMapper;
import com.miabogado.domain.subscription.repository.SubscriptionPlanRepository;
import com.miabogado.domain.subscription.repository.SubscriptionRepository;
import com.miabogado.domain.tenant.entity.Tenant;
import com.miabogado.domain.tenant.entity.TenantStatus;
import com.miabogado.domain.user.repository.UserRepository;
import com.miabogado.shared.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ciclo de vida de la suscripcion de una firma a la plataforma.
 * <p>
 * <b>Estado de suscripcion vs estado de firma:</b> {@code subscription.status} es la
 * verdad comercial; {@code tenant.status} es la puerta de acceso que mira el login.
 * Se mantienen sincronizados aqui, en un solo sitio, para que el login no tenga que
 * unir dos tablas en cada peticion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> listActivePlans() {
        return subscriptionMapper.toPlanResponses(planRepository.findByActiveTrueOrderBySortOrderAsc());
    }

    /**
     * Alta de la suscripcion al crear la firma. La llama {@code TenantService}.
     */
    @Transactional
    public Subscription createFor(Tenant tenant, UUID planId) {
        SubscriptionPlan plan = activePlan(planId);
        Subscription subscription = subscriptionRepository.save(new Subscription(tenant, plan));
        syncTenantStatus(subscription);
        return subscription;
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse findCurrent(UUID tenantId) {
        Subscription subscription = requireCurrent(tenantId);
        return subscriptionMapper.toResponse(subscription, userRepository.countActiveMembers(tenantId));
    }

    /**
     * Cambio de plan. Un downgrade que dejaria a la firma por encima del limite
     * se rechaza: es mejor un error claro que desactivar usuarios en silencio.
     */
    @Transactional
    public SubscriptionResponse changePlan(UUID tenantId, UUID planId) {
        Subscription subscription = requireCurrent(tenantId);
        SubscriptionPlan newPlan = activePlan(planId);

        long members = userRepository.countActiveMembers(tenantId);
        if (newPlan.getMaxMembers() != null && members > newPlan.getMaxMembers()) {
            throw BusinessException.conflict(
                    "El plan %s admite %d miembros y la firma tiene %d activos. Desactiva miembros antes de cambiar."
                            .formatted(newPlan.getName(), newPlan.getMaxMembers(), members));
        }

        subscription.changePlan(newPlan);
        syncTenantStatus(subscription);
        return subscriptionMapper.toResponse(subscription, members);
    }

    /**
     * Guarda de aforo. La invocan los altas de miembro antes de crear nada.
     */
    @Transactional(readOnly = true)
    public void ensureCanAddMember(UUID tenantId) {
        Subscription subscription = requireCurrent(tenantId);
        long members = userRepository.countActiveMembers(tenantId);

        if (!subscription.getPlan().allowsOneMoreMember(members)) {
            throw BusinessException.conflict(
                    "El plan %s permite %d miembros. Cambia de plan para agregar mas."
                            .formatted(subscription.getPlan().getName(), subscription.getPlan().getMaxMembers()));
        }
    }

    @Transactional
    public void cancel(UUID tenantId) {
        Subscription subscription = requireCurrent(tenantId);
        subscription.cancel(Instant.now());
        syncTenantStatus(subscription);
    }

    /**
     * Pruebas vencidas: la firma queda suspendida (solo lectura) hasta que pague.
     * Cuando entre la pasarela de pago, aqui es donde se enganchara el cobro.
     */
    @Transactional
    public int expireFinishedTrials() {
        List<Subscription> expired = subscriptionRepository
                .findByStatusAndTrialEndsAtBefore(SubscriptionStatus.TRIALING, Instant.now());

        expired.forEach(subscription -> {
            subscription.markPastDue();
            syncTenantStatus(subscription);
        });

        if (!expired.isEmpty()) {
            log.info("{} periodos de prueba vencidos pasados a PAST_DUE", expired.size());
        }
        return expired.size();
    }

    private Subscription requireCurrent(UUID tenantId) {
        return subscriptionRepository.findCurrentByTenantId(tenantId)
                .orElseThrow(() -> BusinessException.notFound("Suscripcion de la firma"));
    }

    private SubscriptionPlan activePlan(UUID planId) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("Plan"));
        if (!plan.isActive()) {
            throw BusinessException.conflict("El plan %s ya no esta disponible".formatted(plan.getName()));
        }
        return plan;
    }

    private void syncTenantStatus(Subscription subscription) {
        TenantStatus tenantStatus = switch (subscription.getStatus()) {
            case TRIALING -> TenantStatus.TRIAL;
            case ACTIVE -> TenantStatus.ACTIVE;
            case PAST_DUE -> TenantStatus.SUSPENDED;
            case CANCELLED -> TenantStatus.CANCELLED;
        };
        subscription.getTenant().setStatus(tenantStatus);
    }
}
