package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.log.util.LoggerUtils;

/**
 * Vi ønsker ikke eksponere detaljerte feilmeldinger frontend. Vi spesialbehandler tilgangsmangel, ellers får alle en generell melding om serverfeil.
 * Legger alltid ved callId så frontend kan vise denne og vi kan finne den igjen i loggene hvis arbeidsgiver melder den inn.
 */
@Provider
public class GeneralRestExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = LoggerFactory.getLogger(GeneralRestExceptionMapper.class);

    @Override
    public Response toResponse(Throwable feil) {
        loggTilApplikasjonslogg(feil);
        if (feil instanceof InntektsmeldingAPIException ex) {
            return Response.status(ex.getStatus())
                .entity(new ErrorResponse(ex.getFeilmelding().getVerdi(), ex.getCallId()))
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorResponse(EksponertFeilmelding.STANDARD_FEIL.getVerdi(), MDCOperations.getCallId()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static void loggTilApplikasjonslogg(Throwable feil) {
        var melding = "Fikk uventet feil: " + getExceptionMelding(feil);
        LOG.warn(melding, feil);
    }

    private static String getExceptionMelding(Throwable feil) {
        return getTextForField(feil.getMessage());
    }

    private static String getTextForField(String input) {
        return input != null ? LoggerUtils.removeLineBreaks(input) : "";
    }
}
