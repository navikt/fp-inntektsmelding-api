package no.nav.foreldrepenger.inntektsmelding.api.server.app.api.jackson;

import com.fasterxml.jackson.core.JsonParseException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.server.rest.FeilUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.exc.StreamReadException;

public class StreamReadExceptionMapper implements ExceptionMapper<StreamReadException> {

    private static final Logger LOG = LoggerFactory.getLogger(StreamReadExceptionMapper.class);

    @Override
    public Response toResponse(StreamReadException exception) {
        var feil = String.format("FIM-299955: JSON-parsing feil: %s", exception.getMessage());
        LOG.warn(feil);
        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(EksponertFeilmelding.SERIALISERINGSFEIL.getVerdi(), MDCOperations.getCallId())).type(
            MediaType.APPLICATION_JSON).build();
    }


}
