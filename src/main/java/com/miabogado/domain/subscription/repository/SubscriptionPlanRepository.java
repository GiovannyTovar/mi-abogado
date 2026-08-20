package com.miabogado.domain.subscription.repository;

import com.miabogado.domain.subscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    List<SubscriptionPlan> findByActiveTrueOrderBySortOrderAsc();

    Optional<SubscriptionPlan> findByCode(String code);
}
