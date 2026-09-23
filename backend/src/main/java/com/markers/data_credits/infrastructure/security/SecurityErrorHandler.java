package com.markers.data_credits.infrastructure.security;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markers.data_credits.infrastructure.exception.ApiResponse;

import reactor.core.publisher.Mono;

/**
 * Respuestas 401/403 generadas por la cadena de filtros de seguridad,
 * con el mismo formato {@link ApiResponse} que el resto de la API.
 */
@Component
public class SecurityErrorHandler implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Sin token o con token inválido/expirado. */
    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return write(exchange, HttpStatus.UNAUTHORIZED, "Debe iniciar sesión o su sesión expiró");
    }

    /** Autenticado pero sin el rol/permiso requerido. */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException ex) {
        return write(exchange, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción");
    }

    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(ApiResponse.error(status, message));
        } catch (JsonProcessingException e) {
            body = ("{\"status\":" + status.value() + ",\"error\":true}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
