package com.markers.data_credits.domain.port.in;

import com.markers.data_credits.domain.model.RateCatalog;

/**
 * Caso de uso: consultar los tramos de tasa vigentes y los límites de tasa EA.
 */
public interface QueryInterestRatesUseCase {

    RateCatalog getCatalog();
}
