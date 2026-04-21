package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import no.nav.foreldrepenger.inntektsmelding.api.server.app.api.jackson.JsonMappingExceptionMapper;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

class JsonMappingExceptionMapperTest {

    @Test
    void skal_mappe_InvalidTypeIdException() {
        var mapper = new JsonMappingExceptionMapper();
        var resultat = mapper.toResponse(new InvalidTypeIdException(null, "Ukjent type-kode", null, "23525"));
        var dto = (ErrorResponse) resultat.getEntity();
        assertThat(dto.feilmelding()).isEqualTo(EksponertFeilmelding.SERIALISERINGSFEIL.getTekst());
    }

}
