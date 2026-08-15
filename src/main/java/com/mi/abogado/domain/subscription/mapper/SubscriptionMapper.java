package com.mi.abogado.domain.subscription.mapper;

import com.mi.abogado.domain.subscription.dto.SubscriptionPlanResponse;
import com.mi.abogado.domain.subscription.dto.SubscriptionResponse;
import com.mi.abogado.domain.subscription.entity.Subscription;
import com.mi.abogado.domain.subscription.entity.SubscriptionPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface SubscriptionMapper {

    SubscriptionPlanResponse toResponse(SubscriptionPlan plan);

    List<SubscriptionPlanResponse> toPlanResponses(List<SubscriptionPlan> plans);

    /** membersInUse no sale de la entidad: lo cuenta el service. */
    @Mapping(target = "membersInUse", source = "membersInUse")
    SubscriptionResponse toResponse(Subscription subscription, long membersInUse);
}
