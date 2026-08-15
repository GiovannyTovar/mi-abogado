package com.mi.abogado.domain.document.service;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/**
 * @param basePath        raiz del almacen de archivos (volumen del contenedor en produccion)
 * @param allowedContentTypes lista blanca de tipos. Un despacho sube PDF, imagenes y
 *                            ofimatica; aceptar cualquier cosa convierte el almacen
 *                            en un vector de distribucion de malware.
 */
@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        @NotBlank String basePath,
        Set<String> allowedContentTypes
) {
}
