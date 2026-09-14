package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import java.util.Set;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.server.rest.RestServerFeilUtils;

/**
 * Vi ønsker ikke eksponere detaljerte feilmeldinger frontend. Vi spesialbehandler tilgangsmangel, ellers får alle en generell melding om serverfeil.
 * Legger alltid ved callId så frontend kan vise denne og vi kan finne den igjen i loggene hvis arbeidsgiver melder den inn.
 */
@Provider
public class LokalRestExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = LoggerFactory.getLogger(LokalRestExceptionMapper.class);
    private static final Set<EksponertFeilmelding> TILGANGSFEIL = Set.of(
        EksponertFeilmelding.MANGLER_TOKEN,
        EksponertFeilmelding.UTGAATT_TOKEN,
        EksponertFeilmelding.UGYLDIG_TOKEN,
        EksponertFeilmelding.FEIL_SCOPE,
        EksponertFeilmelding.IKKE_TILGANG_ALTINN);

    @Override
    public Response toResponse(Throwable feil) {
        if (feil instanceof InntektsmeldingAPIException ex && TILGANGSFEIL.contains(ex.getFeilmelding())) {
            LOG.info("Avvist kall: {}", ex.getFeilmelding());
        } else {
            RestServerFeilUtils.loggFeil(feil);
        }
        if (feil instanceof InntektsmeldingAPIException ex) {
            return Response.status(ex.getStatus())
                .entity(new ErrorResponse(ex.getFeilmelding().name(), ex.getFeilmelding().getTekst()))
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorResponse(EksponertFeilmelding.STANDARD_FEIL.name(), EksponertFeilmelding.STANDARD_FEIL.getTekst()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
