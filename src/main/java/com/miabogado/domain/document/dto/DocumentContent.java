package com.miabogado.domain.document.dto;

import org.springframework.core.io.Resource;

/**
 * Un archivo listo para devolver: el flujo mas los datos de la cabecera HTTP.
 * Evita que el controller tenga que volver a consultar los metadatos.
 */
public record DocumentContent(Resource resource, String fileName, String contentType) {
}
