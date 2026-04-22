package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import no.nav.vedtak.server.rest.FeilUtils;

import no.nav.vedtak.log.mdc.MDCOperations;

/**
 * Vi ønsker ikke eksponere detaljerte feilmeldinger frontend. Vi spesialbehandler tilgangsmangel, ellers får alle en generell melding om serverfeil.
 * Legger alltid ved callId så frontend kan vise denne og vi kan finne den igjen i loggene hvis arbeidsgiver melder den inn.
 */
@Provider
public class LokalRestExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable feil) {
        FeilUtils.loggFeil(feil);
        if (feil instanceof InntektsmeldingAPIException ex) {
            return Response.status(ex.getStatus())
                .entity(new ErrorResponse(ex.getFeilmelding().name(), ex.getFeilmelding().getTekst(), ex.getCallId()))
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorResponse(EksponertFeilmelding.STANDARD_FEIL.name(), EksponertFeilmelding.STANDARD_FEIL.getTekst() + ": " + feil.getMessage(), MDCOperations.getCallId()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
