package com.mi.abogado.domain.client.dto;

import com.mi.abogado.domain.client.entity.ClientType;
import com.mi.abogado.domain.client.entity.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotNull ClientType clientType,
        @NotNull DocumentType documentType,
        @NotBlank @Size(max = 30) String documentNumber,
        @NotBlank @Size(max = 180) String name,
        @Email @Size(max = 180) String email,
        @Size(max = 30) String phone,
        @Size(max = 250) String address,
        @Size(max = 100) String city,
        @Size(max = 4000) String notes
) {
}
