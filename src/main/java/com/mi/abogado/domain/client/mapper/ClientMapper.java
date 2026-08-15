package com.mi.abogado.domain.client.mapper;

import com.mi.abogado.domain.client.dto.ClientResponse;
import com.mi.abogado.domain.client.entity.Client;
import org.mapstruct.Mapper;

@Mapper
public interface ClientMapper {

    ClientResponse toResponse(Client client);
}
