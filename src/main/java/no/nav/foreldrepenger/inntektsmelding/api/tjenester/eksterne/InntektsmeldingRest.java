package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.integrasjoner.FpinntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.api.server.auth.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;
import no.nav.vedtak.log.mdc.MDCOperations;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(InntektsmeldingRest.BASE_PATH)
public class InntektsmeldingRest {
    public static final String BASE_PATH = "/inntektsmelding";
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingRest.class);
    private static final String SEND_INNTEKTSMELDING = "/send-inn";
    private FpinntektsmeldingTjeneste fpinntektsmeldingTjeneste;
    private Tilgang tilgang;

    InntektsmeldingRest() {
        // for CDI proxy
    }

    @Inject
    public InntektsmeldingRest(FpinntektsmeldingTjeneste fpinntektsmeldingTjeneste, Tilgang tilgang) {
        this.fpinntektsmeldingTjeneste = fpinntektsmeldingTjeneste;
        this.tilgang = tilgang;
    }

    @POST
    @Path(SEND_INNTEKTSMELDING)
    public Response sendInntektsmelding(@Valid @NotNull InntektsmeldingRequest inntektsmeldingRequest) {
        LOG.info("Mottatt inntektsmelding for forespørselUuid {} ", inntektsmeldingRequest.foresporselUuid());
        var forespørsel = fpinntektsmeldingTjeneste.hentForespørsel(inntektsmeldingRequest.foresporselUuid());

        if (forespørsel == null) {
            LOG.warn("Avvist inntektsmelding for forespørselUuid {}. Forespørsel ikke funnet.", inntektsmeldingRequest.foresporselUuid());
            return Response.ok(new ErrorResponse(EksponertFeilmelding.TOM_FORESPØRSEL.getVerdi(), MDCOperations.getCallId())).build();
        }
        if (forespørsel.status() == Forespørsel.ForespørselStatus.UTGÅTT) {
            LOG.warn("Avvist inntektsmelding for forespørselUuid {}. Forespørsel har status UTGÅTT.", inntektsmeldingRequest.foresporselUuid());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(EksponertFeilmelding.UGYLDIG_FORESPØRSEL.getVerdi(), MDCOperations.getCallId()))
                .build();
        }

        var feilmelding = validerInntektsmeldingMotForespørsel(inntektsmeldingRequest, forespørsel);
        if (feilmelding.isPresent()) {
            LOG.warn("Avvist inntektsmelding for forespørselUuid {}. Data i inntektsmelding samsvarer ikke med data i forespørsel.",
                inntektsmeldingRequest.foresporselUuid());
            return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(feilmelding.get().getVerdi(), MDCOperations.getCallId())).build();
        }

        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(forespørsel.orgnummer()));

        //Todo Sende inntektsmelding videre til fpinntektsmelding

        //Må gi et svar tilbake
        return Response.ok().build();
    }

    private Optional<EksponertFeilmelding> validerInntektsmeldingMotForespørsel(InntektsmeldingRequest inntektsmeldingApiDto, Forespørsel forespørsel) {

        if (!inntektsmeldingApiDto.startdato().equals(forespørsel.førsteUttaksdato())) {
            LOG.warn("Første uttaksdato fra inntektsmelding {} og første uttaksdato fra forespørsel {} matcher ikke.",
                inntektsmeldingApiDto.startdato(),
                forespørsel.førsteUttaksdato());
            return Optional.of(EksponertFeilmelding.MISMATCH_FØRSTE_UTTAKSDATO);
        }
        if (!mapYtelseType(inntektsmeldingApiDto.ytelse()).equals(forespørsel.ytelseType())) {
            LOG.warn("Ytelsetype fra inntektsmelding {} og ytelsetype fra forespørsel {} matcher ikke.",
                inntektsmeldingApiDto.ytelse(),
                forespørsel.ytelseType());
            return Optional.of(EksponertFeilmelding.MISMATCH_YTELSE);
        }
        return Optional.empty();
    }

    private YtelseTypeDto mapYtelseType(InntektsmeldingRequest.YtelseType ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
        };
    }
}



