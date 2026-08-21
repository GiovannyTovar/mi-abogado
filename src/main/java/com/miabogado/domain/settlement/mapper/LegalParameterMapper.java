package com.miabogado.domain.settlement.mapper;

import com.miabogado.domain.settlement.dto.LegalParameterResponse;
import com.miabogado.domain.settlement.entity.LegalParameter;
import org.mapstruct.Mapper;

@Mapper
public interface LegalParameterMapper {

    LegalParameterResponse toResponse(LegalParameter parameter);
}
