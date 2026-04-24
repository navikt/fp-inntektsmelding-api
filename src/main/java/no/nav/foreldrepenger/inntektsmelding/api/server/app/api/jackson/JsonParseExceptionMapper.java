package no.nav.foreldrepenger.inntektsmelding.api.server.app.api.jackson;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;

import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.vedtak.log.mdc.MDCOperations;

/**
 * Håndterer JsonParsingException ved serialisering av innkommende JSON.
 * Logger feildetaljer  og returnerer feilmelding til klient.
 */
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    private static final Logger LOG = LoggerFactory.getLogger(JsonParseExceptionMapper.class);

    @Override
    public Response toResponse(JsonParseException exception) {
        var feil = String.format("FIM-299955: JSON-parsing feil: %s", exception.getMessage());
        LOG.warn(feil);
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(EksponertFeilmelding.SERIALISERINGSFEIL.name(),
                EksponertFeilmelding.SERIALISERINGSFEIL.getTekst() + ": " + exception.getMessage(),
                MDCOperations.getCallId()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
