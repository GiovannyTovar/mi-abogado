package com.miabogado.domain.tenant.mapper;

import com.miabogado.domain.tenant.dto.TenantResponse;
import com.miabogado.domain.tenant.entity.Tenant;
import org.mapstruct.Mapper;

@Mapper
public interface TenantMapper {

    TenantResponse toResponse(Tenant tenant);
}
