package com.miabogado.domain.tenant.service;

import com.miabogado.domain.subscription.service.SubscriptionService;
import com.miabogado.domain.tenant.dto.CreateTenantRequest;
import com.miabogado.domain.tenant.dto.TenantResponse;
import com.miabogado.domain.tenant.dto.TenantSummary;
import com.miabogado.domain.tenant.dto.UpdateTenantRequest;
import com.miabogado.domain.tenant.entity.Tenant;
import com.miabogado.domain.tenant.entity.TenantStatus;
import com.miabogado.domain.tenant.mapper.TenantMapper;
import com.miabogado.domain.tenant.repository.TenantRepository;
import com.miabogado.domain.user.entity.Role;
import com.miabogado.domain.user.entity.User;
import com.miabogado.domain.user.repository.UserRepository;
import com.miabogado.shared.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/**
 * Alta y administracion de bufetes. Las altas las hace el super-admin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private static final int MAX_SLUG_LENGTH = 60;

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final TenantMapper tenantMapper;

    /**
     * Onboarding completo en una transaccion: firma + dueno invitado + suscripcion.
     * Si algo falla, no queda una firma a medias sin dueno o sin plan.
     */
    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        Tenant tenant = tenantRepository.save(new Tenant(request.name(), uniqueSlug(request.name())));
        tenant.setNit(request.nit());
        tenant.setContactEmail(request.ownerEmail());
        tenant.setContactPhone(request.contactPhone());

        User owner = new User(tenant, request.ownerEmail(), request.ownerFullName(), Role.FIRM_OWNER);
        userRepository.save(owner);

        // Fija tenant.status segun el plan (TRIAL si hay prueba, ACTIVE si no).
        subscriptionService.createFor(tenant, request.planId());

        log.info("Firma {} creada con slug {}", tenant.getName(), tenant.getSlug());
        return tenantMapper.toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public Page<TenantSummary> search(TenantStatus status, String search, Pageable pageable) {
        return tenantRepository.search(status, search, pageable);
    }

    @Transactional(readOnly = true)
    public TenantResponse findById(UUID id) {
        return tenantRepository.findById(id)
                .map(tenantMapper::toResponse)
                .orElseThrow(() -> BusinessException.notFound("Firma"));
    }

    @Transactional
    public TenantResponse update(UUID id, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Firma"));

        if (request.name() != null) {
            tenant.setName(request.name());
        }
        if (request.nit() != null) {
            tenant.setNit(request.nit());
        }
        if (request.contactEmail() != null) {
            tenant.setContactEmail(request.contactEmail());
        }
        if (request.contactPhone() != null) {
            tenant.setContactPhone(request.contactPhone());
        }
        return tenantMapper.toResponse(tenant);
    }

    /**
     * Suspension manual del super-admin (incumplimiento, fraude). El impago
     * automatico lo maneja {@code SubscriptionService}.
     */
    @Transactional
    public TenantResponse changeStatus(UUID id, TenantStatus status) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Firma"));
        tenant.setStatus(status);
        return tenantMapper.toResponse(tenant);
    }

    /**
     * "Ramirez & Asociados S.A.S." -> "ramirez-asociados-sas", con sufijo numerico
     * si ya existe. Sera el subdominio del portal white-label.
     */
    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (base.isBlank()) {
            base = "firma";
        }
        if (base.length() > MAX_SLUG_LENGTH) {
            base = base.substring(0, MAX_SLUG_LENGTH);
        }

        String candidate = base;
        int suffix = 2;
        while (tenantRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
