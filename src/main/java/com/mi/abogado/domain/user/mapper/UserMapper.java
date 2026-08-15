package com.mi.abogado.domain.user.mapper;

import com.mi.abogado.domain.user.dto.UserResponse;
import com.mi.abogado.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    @Mapping(target = "tenantId", source = "tenant.id")
    UserResponse toResponse(User user);
}
