package com.miabogado.domain.user.mapper;

import com.miabogado.domain.user.dto.UserResponse;
import com.miabogado.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    @Mapping(target = "tenantId", source = "tenant.id")
    UserResponse toResponse(User user);
}
