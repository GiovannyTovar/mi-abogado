package com.mi.abogado.domain.subscription.controller;

import com.mi.abogado.domain.subscription.dto.ChangePlanRequest;
import com.mi.abogado.domain.subscription.dto.SubscriptionPlanResponse;
import com.mi.abogado.domain.subscription.dto.SubscriptionResponse;
import com.mi.abogado.domain.subscription.service.SubscriptionService;
import com.mi.abogado.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /** Publico: la landing y el onboarding necesitan pintar los planes sin sesion. */
    @GetMapping("/public/plans")
    public List<SubscriptionPlanResponse> listPlans() {
        return subscriptionService.listActivePlans();
    }

    @GetMapping("/subscription")
    @PreAuthorize("hasAnyRole('FIRM_OWNER', 'LAWYER', 'ASSISTANT')")
    public SubscriptionResponse current() {
        return subscriptionService.findCurrent(TenantContext.require());
    }

    @PutMapping("/subscription/plan")
    @PreAuthorize("hasRole('FIRM_OWNER')")
    public SubscriptionResponse changePlan(@Valid @RequestBody ChangePlanRequest request) {
        return subscriptionService.changePlan(TenantContext.require(), request.planId());
    }

    @PostMapping("/subscription/cancel")
    @PreAuthorize("hasRole('FIRM_OWNER')")
    public void cancel() {
        subscriptionService.cancel(TenantContext.require());
    }
}
