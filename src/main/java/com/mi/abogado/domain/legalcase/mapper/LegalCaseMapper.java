package com.mi.abogado.domain.legalcase.mapper;

import com.mi.abogado.domain.legalcase.dto.CaseDeadlineResponse;
import com.mi.abogado.domain.legalcase.dto.CaseResponse;
import com.mi.abogado.domain.legalcase.entity.CaseDeadline;
import com.mi.abogado.domain.legalcase.entity.LegalCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface LegalCaseMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "assignedLawyerId", source = "assignedLawyer.id")
    @Mapping(target = "assignedLawyerName", source = "assignedLawyer.user.fullName")
    @Mapping(target = "practiceAreaId", source = "practiceArea.id")
    @Mapping(target = "practiceAreaName", source = "practiceArea.name")
    CaseResponse toResponse(LegalCase legalCase);

    CaseDeadlineResponse toResponse(CaseDeadline deadline);

    List<CaseDeadlineResponse> toDeadlineResponses(List<CaseDeadline> deadlines);
}
