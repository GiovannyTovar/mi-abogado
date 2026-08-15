package com.mi.abogado.domain.tenant.mapper;

import com.mi.abogado.domain.tenant.dto.TenantResponse;
import com.mi.abogado.domain.tenant.entity.Tenant;
import org.mapstruct.Mapper;

@Mapper
public interface TenantMapper {

    TenantResponse toResponse(Tenant tenant);
}
