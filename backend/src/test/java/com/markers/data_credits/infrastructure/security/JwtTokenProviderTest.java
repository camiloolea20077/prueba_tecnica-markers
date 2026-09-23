package com.markers.data_credits.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.markers.data_credits.domain.model.AuthToken;
import com.markers.data_credits.support.TestUsers;

class JwtTokenProviderTest {

    private static final String SECRET =
            "cHJ1ZWJhLXRlY25pY2EtbWFya2Vycy1wcmVzdGFtb3MtYmFuY2FyaW9zLWp3dC1zZWNyZXQtMjU2";
    private static final String OTHER_SECRET =
            "b3RyYS1jbGF2ZS1kaXN0aW50YS1wYXJhLWZpcm1hci10b2tlbnMtZGUtcHJ1ZWJhLWp3dC0yNTY=";

    private final JwtTokenProvider provider = provider(SECRET, 60);

    private static JwtTokenProvider provider(String secret, long minutes) {
        return new JwtTokenProvider(new SecurityProperties(
                new SecurityProperties.Jwt(secret, minutes),
                new SecurityProperties.Cors(List.of("http://localhost:4200"))));
    }

    @Test
    @DisplayName("el token emitido contiene id, email, rol y permisos")
    void generateAndParse() {
        AuthToken token = provider.generate(TestUsers.admin());

        Optional<AuthenticatedUser> parsed = provider.parse(token.value());

        assertThat(parsed).isPresent();
        AuthenticatedUser user = parsed.get();
        assertThat(user.id()).isEqualTo(2L);
        assertThat(user.email()).isEqualTo("admin@test.com");
        assertThat(user.role()).isEqualTo("ADMIN");
        assertThat(user.permissions()).contains("CREDIT_APPROVE", "CREDIT_REJECT");
    }

    @Test
    @DisplayName("token firmado con otra clave es rechazado")
    void rejectsForeignSignature() {
        String foreign = provider(OTHER_SECRET, 60).generate(TestUsers.admin()).value();

        assertThat(provider.parse(foreign)).isEmpty();
    }

    @Test
    @DisplayName("token expirado es rechazado")
    void rejectsExpired() {
        String expired = provider(SECRET, -1).generate(TestUsers.user()).value();

        assertThat(provider.parse(expired)).isEmpty();
    }

    @Test
    @DisplayName("token con un carácter extra al final es rechazado")
    void rejectsTrailingCharacter() {
        String token = provider.generate(TestUsers.user()).value();

        assertThat(provider.parse(token + "x")).isEmpty();
    }

    @Test
    @DisplayName("valores basura o vacíos son rechazados sin lanzar excepción")
    void rejectsGarbage() {
        assertThat(provider.parse("")).isEmpty();
        assertThat(provider.parse(null)).isEmpty();
        assertThat(provider.parse("no.es.jwt")).isEmpty();
    }
}
