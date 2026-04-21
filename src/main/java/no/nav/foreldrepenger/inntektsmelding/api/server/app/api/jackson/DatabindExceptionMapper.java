package no.nav.foreldrepenger.inntektsmelding.api.server.app.api.jackson;

import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.server.rest.FeilUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.DatabindException;

public class DatabindExceptionMapper implements ExceptionMapper<DatabindException> {

    private static final Logger LOG = LoggerFactory.getLogger(DatabindExceptionMapper.class);

    @Override
    public Response toResponse(DatabindException exception) {
        var feil = "FIM-252294: JSON-mapping feil";
        LOG.warn(feil);
        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(EksponertFeilmelding.SERIALISERINGSFEIL.getVerdi(), MDCOperations.getCallId())).type(
            MediaType.APPLICATION_JSON).build();
    }

}
