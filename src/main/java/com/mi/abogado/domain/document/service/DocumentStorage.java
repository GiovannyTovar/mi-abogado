package com.mi.abogado.domain.document.service;

import com.mi.abogado.shared.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Almacen de binarios en disco (volumen del VPS).
 * <p>
 * Es el unico punto del sistema que toca el sistema de archivos. Si algun dia se
 * pasa a S3 o similar, se cambia esta clase y nada mas; por eso no hay interfaz:
 * hoy hay una sola implementacion real y una interfaz con un solo implementador
 * no aporta nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentStorage {

    private final StorageProperties properties;

    /**
     * Guarda el archivo y devuelve su clave.
     * <p>
     * La clave no contiene el nombre original: se genera con un UUID. Asi un
     * nombre malicioso ({@code ../../etc/passwd}) no puede escapar del directorio,
     * y dos archivos homonimos no se pisan.
     */
    public String store(MultipartFile file, UUID tenantId, UUID caseId) {
        validateContentType(file.getContentType());

        String key = "%s/%s/%s%s".formatted(tenantId, caseId, UUID.randomUUID(), extensionOf(file));
        Path target = resolve(key);

        try {
            Files.createDirectories(target.getParent());
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return key;
        } catch (IOException e) {
            log.error("No se pudo guardar el archivo {}", key, e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el archivo");
        }
    }

    public Resource load(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.isReadable(path)) {
            throw BusinessException.notFound("Archivo");
        }
        return new PathResource(path);
    }

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            // El registro ya se borro; un huerfano en disco no justifica tumbar la operacion.
            log.warn("No se pudo borrar el archivo {}", storageKey, e);
        }
    }

    /**
     * Resuelve la clave dentro del almacen y comprueba que no se salga de el:
     * la ultima defensa contra un path traversal.
     */
    private Path resolve(String storageKey) {
        Path base = Path.of(properties.basePath()).toAbsolutePath().normalize();
        Path resolved = base.resolve(storageKey).normalize();

        if (!resolved.startsWith(base)) {
            throw BusinessException.forbidden("Ruta de archivo invalida");
        }
        return resolved;
    }

    private void validateContentType(String contentType) {
        var allowed = properties.allowedContentTypes();
        if (allowed == null || allowed.isEmpty()) {
            return;
        }
        if (contentType == null || !allowed.contains(contentType)) {
            throw BusinessException.conflict("Tipo de archivo no permitido: " + contentType);
        }
    }

    private String extensionOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null) {
            return "";
        }
        int dot = original.lastIndexOf('.');
        // Solo extensiones cortas y alfanumericas: nada de lo que venga del nombre
        // original llega al disco sin filtrar.
        if (dot < 0 || dot == original.length() - 1) {
            return "";
        }
        String extension = original.substring(dot + 1);
        return extension.matches("[A-Za-z0-9]{1,10}") ? "." + extension.toLowerCase() : "";
    }
}
