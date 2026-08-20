package com.miabogado.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** ID token que devuelve el SDK de Firebase en el cliente Ionic. */
public record GoogleLoginRequest(@NotBlank String idToken) {
}
