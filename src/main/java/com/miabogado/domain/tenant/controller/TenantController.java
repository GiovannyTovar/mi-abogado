package com.miabogado.domain.tenant.controller;

import com.miabogado.domain.tenant.dto.CreateTenantRequest;
import com.miabogado.domain.tenant.dto.TenantResponse;
import com.miabogado.domain.tenant.dto.TenantSummary;
import com.miabogado.domain.tenant.dto.UpdateTenantRequest;
import com.miabogado.domain.tenant.entity.TenantStatus;
import com.miabogado.domain.tenant.service.TenantService;
import com.miabogado.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    // --- Administracion de plataforma (super-admin) ---

    @PostMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        TenantResponse created = tenantService.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/tenants/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @GetMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Page<TenantSummary> search(@RequestParam(required = false) TenantStatus status,
                                      @RequestParam(required = false) String search,
                                      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                      Pageable pageable) {
        return tenantService.search(status, search, pageable);
    }

    @GetMapping("/tenants/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public TenantResponse findById(@PathVariable UUID id) {
        return tenantService.findById(id);
    }

    @PatchMapping("/tenants/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public TenantResponse changeStatus(@PathVariable UUID id, @RequestParam TenantStatus status) {
        return tenantService.changeStatus(id, status);
    }

    // --- La firma sobre si misma ---

    /** El id sale del token, no de la URL: nadie puede pedir la ficha de otra firma. */
    @GetMapping("/firm")
    @PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
    public TenantResponse myFirm() {
        return tenantService.findById(TenantContext.require());
    }

    @PatchMapping("/firm")
    @PreAuthorize("hasRole('FIRM_OWNER')")
    public TenantResponse updateMyFirm(@Valid @RequestBody UpdateTenantRequest request) {
        return tenantService.update(TenantContext.require(), request);
    }
}
