package com.markers.data_credits.domain.port.out;

import com.markers.data_credits.domain.model.AuthToken;
import com.markers.data_credits.domain.model.User;

/**
 * Puerto de salida para emitir tokens de acceso.
 */
public interface TokenProviderPort {

    AuthToken generate(User user);
}
