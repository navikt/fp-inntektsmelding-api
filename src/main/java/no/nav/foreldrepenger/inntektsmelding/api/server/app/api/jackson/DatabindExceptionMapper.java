package no.nav.foreldrepenger.inntektsmelding.api.server.app.api.jackson;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.vedtak.server.rest.FeilUtils;
import tools.jackson.databind.DatabindException;

public class DatabindExceptionMapper implements ExceptionMapper<DatabindException> {

    private static final Logger LOG = LoggerFactory.getLogger(DatabindExceptionMapper.class);

    @Override
    public Response toResponse(DatabindException exception) {
        FeilUtils.ensureCallId();
        LOG.warn("FIM-252294: JSON-mapping feil: {}", exception.getMessage());

        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(
                EksponertFeilmelding.SERIALISERINGSFEIL.name(),
                EksponertFeilmelding.SERIALISERINGSFEIL.getTekst() + ": " + exception.getMessage(),
                null))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

}
