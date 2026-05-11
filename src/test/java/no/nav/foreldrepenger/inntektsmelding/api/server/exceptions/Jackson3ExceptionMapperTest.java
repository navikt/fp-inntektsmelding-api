package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inntektsmelding.api.server.app.api.Jackson3ExceptionMapper;
import tools.jackson.databind.exc.InvalidTypeIdException;

class Jackson3ExceptionMapperTest {

    @Test
    void skal_mappe_InvalidTypeIdException() {
        var mapper = new Jackson3ExceptionMapper();
        try (var resultat = mapper.toResponse(new InvalidTypeIdException(null, "Ukjent type-kode", null, "23525"))) {
            var dto = (ErrorResponse) resultat.getEntity();
            assertThat(dto.feilmelding()).startsWith("Serialiseringsfeil: Ukjent type-kode");
        }
    }
}
