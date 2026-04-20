package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.api.integrasjoner.FpinntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.api.server.auth.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.typer.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.vedtak.log.mdc.MDCOperations;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Path(ForespørselRest.BASE_PATH)
public class ForespørselRest {
    public static final String BASE_PATH = "/forespoersel";
    private static final String HENT_FORESPØRSEL = "/{uuid}";
    private static final String HENT_FLERE = "/forespoersler";
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
    public Response hentForespørsel(@NotNull @Valid @PathParam("uuid") @Pattern(regexp = "^[a-fA-F\\d]{8}(?:-[a-fA-F\\d]{4}){3}-[a-fA-F\\d]{12}$", message = "Ugyldig UUID-format") String forespørselUuid) {
        LOG.info("Innkomende kall på hent forespørsel {}", forespørselUuid);
        var uuid = UUID.fromString(forespørselUuid);

        Forespørsel forespørsel = fpinntektsmeldingTjeneste.hentForespørsel(uuid);
        if (forespørsel == null) {
            return Response.ok(new ErrorResponse(EksponertFeilmelding.TOM_FORESPOERSEL.name(), EksponertFeilmelding.TOM_FORESPOERSEL.getTekst(), MDCOperations.getCallId())).build();
        }

        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(forespørsel.orgnummer());
        var dto = mapTilDto(forespørsel);
        return Response.ok(dto).build();
    }


    @POST
    @Path(HENT_FLERE)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response hentForespørsler(@NotNull @Valid ForespørselFilter filterRequest) {
        LOG.info("Innkomende kall på søk etter forespørsler");

        // Det er spurt etter en spesifikk forespørsel, henter kun denne
        if (filterRequest.forespørselId() != null) {
            Forespørsel forespørsel = fpinntektsmeldingTjeneste.hentForespørsel(filterRequest.forespørselId());
            if (forespørsel == null) {
                return Response.ok(List.of()).build();
            }
            tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(forespørsel.orgnummer());
            return Response.ok(List.of(mapTilDto(forespørsel))).build();
        }

        if (datoerErUgyldige(filterRequest)) {
            return Response.status(Response.Status.BAD_REQUEST)
                 .entity(new ErrorResponse(EksponertFeilmelding.UGYLDIG_PERIODE.name(), EksponertFeilmelding.UGYLDIG_PERIODE.getTekst(), MDCOperations.getCallId()))
                 .build();
        }

        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(filterRequest.orgnr()));
        var forespørsler = fpinntektsmeldingTjeneste.hentForespørsler(filterRequest.orgnr(),
            filterRequest.fnr(),
            filterRequest.status(),
            filterRequest.ytelseType(),
            filterRequest.fom(),
            filterRequest.tom());

        var dtoer = forespørsler.stream().map(this::mapTilDto).toList();

        return Response.ok(dtoer).build();
    }

    private boolean datoerErUgyldige(ForespørselFilter filterRequest) {
        return filterRequest.fom() != null && filterRequest.tom() != null && filterRequest.fom().isAfter(filterRequest.tom());
    }

    private ForespørselDto mapTilDto(Forespørsel forespørsel) {
        return new ForespørselDto(forespørsel.forespørselUuid(),
            forespørsel.orgnummer().orgnr(),
            forespørsel.fødselsnummer(),
            forespørsel.førsteUttaksdato(),
            forespørsel.skjæringstidspunkt(),
            KodeverkMapper.mapTilDto(forespørsel.status()),
            KodeverkMapper.mapTilDto(forespørsel.ytelseType()),
            forespørsel.opprettetTid());
    }
}
