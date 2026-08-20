package com.miabogado;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Arranca el contexto contra un Postgres real: Flyway aplica V1 y V2, y despues
 * {@code ddl-auto: validate} comprueba que las entidades encajan con ese esquema.
 * Si una migracion y una entidad se desincronizan, este test lo detecta.
 * <p>
 * Requiere Docker en la maquina.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class AbogadoApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Test
	void contextLoads() {
	}

}
