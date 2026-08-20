package com.miabogado.domain.lawyer.mapper;

import com.miabogado.domain.lawyer.dto.LawyerResponse;
import com.miabogado.domain.lawyer.dto.PracticeAreaResponse;
import com.miabogado.domain.lawyer.entity.Lawyer;
import com.miabogado.domain.lawyer.entity.PracticeArea;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * El componentModel spring y la politica de campos sin mapear (ERROR) se
 * configuran una sola vez en el compiler plugin del pom.
 */
@Mapper
public interface LawyerMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "phone", source = "user.phone")
    @Mapping(target = "photoUrl", source = "user.photoUrl")
    LawyerResponse toResponse(Lawyer lawyer);

    PracticeAreaResponse toResponse(PracticeArea practiceArea);

    List<PracticeAreaResponse> toPracticeAreaResponses(List<PracticeArea> practiceAreas);
}
