package com.mi.abogado.domain.subscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Revisa a diario los periodos de prueba vencidos.
 * <p>
 * Con una sola instancia de la app en el VPS basta con {@code @Scheduled}. Si algun
 * dia hay mas de un nodo, esto necesitara un lock (ShedLock o equivalente) para no
 * ejecutarse dos veces.
 */
@Component
@RequiredArgsConstructor
public class TrialExpirationJob {

    private final SubscriptionService subscriptionService;

    /** Todos los dias a las 03:00, hora de Bogota. */
    @Scheduled(cron = "0 0 3 * * *", zone = "America/Bogota")
    public void expireFinishedTrials() {
        subscriptionService.expireFinishedTrials();
    }
}
