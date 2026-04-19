package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonMappingException;

import no.nav.vedtak.log.mdc.MDCOperations;

// Vinner over den fra felles. Men må snakke om feil-entiteter her - konsumenten har sendt noe feil og trenger debugge.
@Priority(1)
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {

    private static final Logger LOG = LoggerFactory.getLogger(JsonMappingExceptionMapper.class);

    @Override
    public Response toResponse(JsonMappingException exception) {
        var feil = "FIM-252294: JSON-mapping feil";
        LOG.warn(feil);
        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(EksponertFeilmelding.SERIALISERINGSFEIL.getVerdi(), MDCOperations.getCallId())).type(MediaType.APPLICATION_JSON).build();
    }
}
