package com.mi.abogado.shared.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Inicializa el Admin SDK una sola vez. Solo se usa para <b>verificar</b> ID tokens
 * de Firebase; la sesion de la app la maneja nuestro propio JWT.
 */
@Slf4j
@Configuration
// Propiedad vacia (tests, arranque local sin Firebase) = no se inicializa el SDK.
// ConditionalOnProperty no sirve aqui: considera "presente" una cadena vacia.
@ConditionalOnExpression("!'${app.firebase.credentials-location:}'.isEmpty()")
public class FirebaseConfig {

    @Bean
    FirebaseApp firebaseApp(FirebaseProperties properties, ResourceLoader resourceLoader) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (InputStream credentials = resourceLoader.getResource(properties.credentialsLocation()).getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .setProjectId(properties.projectId())
                    .build();
            log.info("Firebase Admin SDK inicializado para el proyecto {}", properties.projectId());
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
