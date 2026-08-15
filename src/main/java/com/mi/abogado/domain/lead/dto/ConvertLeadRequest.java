package com.mi.abogado.domain.lead.dto;

import com.mi.abogado.domain.client.entity.ClientType;
import com.mi.abogado.domain.client.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Convertir el lead en cliente. Del lead salen nombre, correo, telefono y ciudad;
 * lo unico que falta es el documento, que en la captacion no se pide.
 *
 * @param openCaseTitle si viene, ademas del cliente se abre su primer expediente.
 *                      Es el flujo real: se convierte al lead justo cuando hay caso.
 */
public record ConvertLeadRequest(
        @NotNull ClientType clientType,
        @NotNull DocumentType documentType,
        @NotBlank @Size(max = 30) String documentNumber,
        @Size(max = 200) String openCaseTitle
) {
}
