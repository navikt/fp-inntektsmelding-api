package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.integrasjoner.FpinntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.api.server.auth.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Path(ForespørselRest.BASE_PATH)
public class ForespørselRest {
    public static final String BASE_PATH = "/forespoersel";
    private static final String HENT_FORESPØRSEL = "/{uuid}";
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselRest.class);
    private FpinntektsmeldingTjeneste fpinntektsmeldingTjeneste;
    private Tilgang tilgang;

    ForespørselRest() {
        // for CDI
    }

    @Inject
    public ForespørselRest(FpinntektsmeldingTjeneste fpinntektsmeldingTjeneste, Tilgang tilgang) {
        this.fpinntektsmeldingTjeneste = fpinntektsmeldingTjeneste;
        this.tilgang = tilgang;
    }

    @GET
    @Path(HENT_FORESPØRSEL)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response hentForespørsel(@NotNull @Valid @PathParam("uuid") UUID forespørselUuid) {
        LOG.info("Innkomende kall på hent forespørsel {}", forespørselUuid);

        Forespørsel forespørsel = fpinntektsmeldingTjeneste.hentForespørsel(forespørselUuid);
        if (forespørsel == null) {
            return Response.ok(EksponertFeilmelding.TOM_FORESPØRSEL).build();
        }
        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(forespørsel.orgnummer()));

        //todo må lage kontrakt for forespørsel, og mappe til den før vi returnerer.
        return Response.ok(forespørsel).build();
    }
}
