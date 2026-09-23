package com.markers.data_credits.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.markers.data_credits.infrastructure.adapter.in.web.mapper.AuthWebMapperImpl;
import com.markers.data_credits.infrastructure.adapter.in.web.mapper.CreditWebMapperImpl;
import com.markers.data_credits.infrastructure.adapter.in.web.support.BlockingExecutor;
import com.markers.data_credits.infrastructure.exception.GlobalHandlerException;
import com.markers.data_credits.infrastructure.security.JwtAuthenticationManager;
import com.markers.data_credits.infrastructure.security.JwtSecurityContextRepository;
import com.markers.data_credits.infrastructure.security.JwtTokenProvider;
import com.markers.data_credits.infrastructure.security.SecurityConfig;
import com.markers.data_credits.infrastructure.security.SecurityErrorHandler;

/**
 * Beans reales de seguridad, errores y mapeo para tests {@code @WebFluxTest}
 * (los casos de uso se simulan con {@code @MockitoBean}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtAuthenticationManager.class,
        JwtSecurityContextRepository.class, SecurityErrorHandler.class, GlobalHandlerException.class,
        BlockingExecutor.class, AuthWebMapperImpl.class, CreditWebMapperImpl.class})
public @interface WebLayerTest {
}
