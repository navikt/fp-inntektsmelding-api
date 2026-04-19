package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;

import no.nav.vedtak.log.mdc.MDCOperations;

// Vinner over den fra felles. Men må snakke om feil-entiteter her - konsumenten har sendt noe feil og trenger debugge.
@Priority(1)
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    private static final Logger LOG = LoggerFactory.getLogger(JsonParseExceptionMapper.class);

    @Override
    public Response toResponse(JsonParseException exception) {
        var feil = String.format("FIM-299955: JSON-parsing feil: %s", exception.getMessage());
        LOG.warn(feil);
        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(EksponertFeilmelding.SERIALISERINGSFEIL.getVerdi(), MDCOperations.getCallId())).type(MediaType.APPLICATION_JSON).build();
    }
}
