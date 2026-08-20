package com.miabogado.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.firebase")
public record FirebaseProperties(String projectId, String credentialsLocation) {

    /** Sin credenciales no se inicializa el SDK (tests, arranque local sin Firebase). */
    public boolean isEnabled() {
        return credentialsLocation != null && !credentialsLocation.isBlank();
    }
}
