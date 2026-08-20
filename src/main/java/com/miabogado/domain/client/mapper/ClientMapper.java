package com.miabogado.domain.client.mapper;

import com.miabogado.domain.client.dto.ClientResponse;
import com.miabogado.domain.client.entity.Client;
import org.mapstruct.Mapper;

@Mapper
public interface ClientMapper {

    ClientResponse toResponse(Client client);
}
