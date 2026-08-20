package com.miabogado.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Los jobs no corren en los tests: alterarian el estado a mitad de una prueba.
 */
@Configuration
@EnableScheduling
@Profile("!test")
public class SchedulingConfig {
}
