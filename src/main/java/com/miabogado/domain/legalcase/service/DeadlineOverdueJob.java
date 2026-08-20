package com.miabogado.domain.legalcase.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cierra el dia anterior: lo que quedo pendiente y ya vencio pasa a MISSED.
 * <p>
 * Aqui se enganchara el aviso por WhatsApp de la Fase 7 (recordar N dias antes
 * segun {@code notifyDaysBefore}, y alertar al vencer).
 */
@Component
@RequiredArgsConstructor
public class DeadlineOverdueJob {

    private final CaseDeadlineService caseDeadlineService;

    /** Todos los dias a las 00:30, hora de Bogota. */
    @Scheduled(cron = "0 30 0 * * *", zone = "America/Bogota")
    public void markOverdueAsMissed() {
        caseDeadlineService.markOverdueAsMissed();
    }
}
