package com.miabogado.domain.legalcase.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Consecutivo del expediente por firma y ano: {@code 2026-0001}.
 * <p>
 * El incremento va en un solo {@code INSERT ... ON CONFLICT DO UPDATE ... RETURNING}:
 * es atomico en Postgres, asi que dos personas creando un caso a la vez obtienen
 * numeros distintos sin necesidad de bloquear nada a mano.
 */
@Component
@RequiredArgsConstructor
public class CaseNumberGenerator {

    private static final String NEXT_NUMBER_SQL = """
            insert into case_number_sequence (tenant_id, year, last_number)
            values (?, ?, 1)
            on conflict (tenant_id, year)
            do update set last_number = case_number_sequence.last_number + 1
            returning last_number
            """;

    private final JdbcClient jdbcClient;

    @Transactional
    public String next(UUID tenantId) {
        short year = (short) LocalDate.now().getYear();

        Integer sequence = jdbcClient.sql(NEXT_NUMBER_SQL)
                .params(tenantId, year)
                .query(Integer.class)
                .single();

        return "%d-%04d".formatted(year, sequence);
    }
}
