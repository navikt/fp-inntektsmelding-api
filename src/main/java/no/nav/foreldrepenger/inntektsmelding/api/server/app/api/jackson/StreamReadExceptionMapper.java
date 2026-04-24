package no.nav.foreldrepenger.inntektsmelding.api.server.app.api.jackson;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import no.nav.vedtak.server.rest.FeilUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.vedtak.log.mdc.MDCOperations;
import tools.jackson.core.exc.StreamReadException;

public class StreamReadExceptionMapper implements ExceptionMapper<StreamReadException> {

    private static final Logger LOG = LoggerFactory.getLogger(StreamReadExceptionMapper.class);

    @Override
    public Response toResponse(StreamReadException exception) {
        FeilUtils.ensureCallId();
        LOG.warn("FIM-299955: JSON-parsing feil: {}", exception.getMessage());

        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(
                EksponertFeilmelding.SERIALISERINGSFEIL.name(),
                EksponertFeilmelding.SERIALISERINGSFEIL.getTekst() + ": " + exception.getMessage(),
                MDCOperations.getCallId()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
