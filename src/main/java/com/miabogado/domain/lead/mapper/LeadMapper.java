package com.miabogado.domain.lead.mapper;

import com.miabogado.domain.lead.dto.LeadResponse;
import com.miabogado.domain.lead.entity.Lead;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface LeadMapper {

    @Mapping(target = "practiceAreaId", source = "practiceArea.id")
    @Mapping(target = "practiceAreaName", source = "practiceArea.name")
    @Mapping(target = "assignedLawyerId", source = "assignedLawyer.id")
    @Mapping(target = "assignedLawyerName", source = "assignedLawyer.user.fullName")
    @Mapping(target = "convertedClientId", source = "convertedClient.id")
    LeadResponse toResponse(Lead lead);
}
