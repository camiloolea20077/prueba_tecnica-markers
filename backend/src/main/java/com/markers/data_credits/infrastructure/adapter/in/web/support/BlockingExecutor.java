package com.markers.data_credits.infrastructure.adapter.in.web.support;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Ejecuta casos de uso bloqueantes (JPA) fuera del event-loop de WebFlux.
 * La transacción y la caché del caso de uso corren completas en el hilo de {@code boundedElastic}.
 */
@Component
public class BlockingExecutor {

    public <T> Mono<T> run(Callable<T> task) {
        return Mono.fromCallable(task).subscribeOn(Schedulers.boundedElastic());
    }
}
