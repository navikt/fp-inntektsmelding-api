package no.nav.foreldrepenger.inntektsmelding.api.server.app.api.jackson;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonMappingException;

import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.vedtak.log.mdc.MDCOperations;

/**
 * Håndterer JsonMappingException ved deserializering av innkommende JSON.
 * Logger feildetaljer og returnerer feilmelding til klient.
 */
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {

    private static final Logger LOG = LoggerFactory.getLogger(JsonMappingExceptionMapper.class);
    private static final String ERROR_CODE = "FIM-252294";

    @Override
    public Response toResponse(JsonMappingException exception) {
        LOG.warn("{}: JSON-mapping feil - {}", ERROR_CODE, exception.getMessage());

        var callId = MDCOperations.getCallId();
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(
                EksponertFeilmelding.SERIALISERINGSFEIL.name(),
                EksponertFeilmelding.SERIALISERINGSFEIL.getTekst() + ": " + exception.getMessage(),
                callId))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
