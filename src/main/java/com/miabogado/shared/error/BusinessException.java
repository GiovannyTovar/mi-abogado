package com.miabogado.shared.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Error esperado de negocio: se traduce a una respuesta HTTP concreta,
 * no a un 500. Los casos frecuentes tienen factorias abajo.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static BusinessException notFound(String resource) {
        return new BusinessException(HttpStatus.NOT_FOUND, resource + " no encontrado");
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, message);
    }
}
