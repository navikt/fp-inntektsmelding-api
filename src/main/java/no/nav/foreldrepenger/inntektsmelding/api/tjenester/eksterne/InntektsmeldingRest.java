package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

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

import no.nav.foreldrepenger.inntektsmelding.api.inntektsmelding.InntektsmeldingMapper;

import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.integrasjoner.FpinntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.api.server.auth.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.vedtak.log.mdc.MDCOperations;

import java.util.UUID;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(InntektsmeldingRest.BASE_PATH)
public class InntektsmeldingRest {
    public static final String BASE_PATH = "/inntektsmelding";
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingRest.class);
    private static final String SEND_INNTEKTSMELDING = "/send-inn";
    private static final String HENT_INNTEKTSMELDING = "/hent/{uuid}";
    private static final String HENT_INNTEKTSMELDINGER = "/hent/inntektsmeldinger";
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
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendInntektsmelding(@Valid @NotNull InntektsmeldingRequest inntektsmeldingRequest) {
        LOG.info("Mottatt inntektsmelding for forespørselUuid {} ", inntektsmeldingRequest.foresporselUuid());
        var forespørsel = fpinntektsmeldingTjeneste.hentForespørsel(inntektsmeldingRequest.foresporselUuid());

        if (forespørsel == null) {
            LOG.info("Avvist inntektsmelding for forespørselUuid {}. Forespørsel ikke funnet.", inntektsmeldingRequest.foresporselUuid());
            return Response.ok(new ErrorResponse(EksponertFeilmelding.TOM_FORESPOERSEL.name(), EksponertFeilmelding.TOM_FORESPOERSEL.getTekst(), MDCOperations.getCallId())).build();
        }

        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(forespørsel.orgnummer().orgnr()));

        var feilmelding = InntektsmeldingValidererUtil.validerInntektsmelding(inntektsmeldingRequest, forespørsel);
        if (feilmelding.isPresent()) {
            LOG.info("Avvist inntektsmelding for forespørselUuid {}. Validering av inntektsmelding feilet. Feilmelding: {}",
                inntektsmeldingRequest.foresporselUuid(), feilmelding.get().getTekst());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(feilmelding.get().name(), feilmelding.get().getTekst(), MDCOperations.getCallId()))
                .build();
        }
        var response = fpinntektsmeldingTjeneste.sendInntektsmelding(inntektsmeldingRequest, forespørsel);

        if (response.success()) {
            return Response.ok(response.inntektsmeldingUuid()).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                //todo skal opprette en feilDto i responsen fra fp-inntektsmelding som kan mappes til errorResponse her
                .entity(new ErrorResponse(null, response.melding(), MDCOperations.getCallId()))
                .build();
        }
    }

    @GET
    @Path(HENT_INNTEKTSMELDING)
    public Response hentInntektsmelding(@NotNull @Valid @PathParam("uuid")
                                        @Pattern(regexp = "^[a-fA-F\\d]{8}(?:-[a-fA-F\\d]{4}){3}-[a-fA-F\\d]{12}$", message = "Ugyldig UUID-format")
                                        String innsendingId) {
        LOG.info("Hent inntektsmelding med innsendingId {} ", innsendingId);
        var inntektsmelding = fpinntektsmeldingTjeneste.hentInntektsmelding(UUID.fromString(innsendingId));

        if (inntektsmelding == null) {
            LOG.info("Avvist inntektsmelding for innsendingId {}. Inntektsmelding ikke funnet.", innsendingId);
            return Response.ok(new ErrorResponse(EksponertFeilmelding.TOM_FORESPOERSEL.name(), EksponertFeilmelding.TOM_FORESPOERSEL.getTekst(), MDCOperations.getCallId())).build();
        }

        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(inntektsmelding.orgnr().orgnr()));

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        return Response.status(Response.Status.OK)
            .entity(dto)
            .build();
    }

    @POST
    @Path(HENT_INNTEKTSMELDINGER)
    public Response hentInntektsmeldinger(@NotNull @Valid InntektsmeldingFilter inntektsmeldingFilter) {
        LOG.info("Innkomende kall på søk etter inntektsmeldinger");
        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(inntektsmeldingFilter.orgnr()));
        if (inntektsmeldingFilter.innsendingId() != null) {
            var inntektsmelding = fpinntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingFilter.innsendingId());
            if (inntektsmelding == null) {
                LOG.info("Inntektsmelding med innsendingId {} ikke funnet.", inntektsmeldingFilter.innsendingId());
                return Response.ok(new ErrorResponse(EksponertFeilmelding.TOM_INNTEKTSMELDING.name(), EksponertFeilmelding.TOM_INNTEKTSMELDING.getTekst(), MDCOperations.getCallId())).build();
            }
            if (datoerErUgyldige(inntektsmeldingFilter)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(EksponertFeilmelding.UGYLDIG_PERIODE.name(), EksponertFeilmelding.UGYLDIG_PERIODE.getTekst(), MDCOperations.getCallId()))
                    .build();
            }

            var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

            return Response.status(Response.Status.OK)
                .entity(dto)
                .build();
        }

        var inntektsmeldinger = fpinntektsmeldingTjeneste.hentInntektsmeldinger(inntektsmeldingFilter.orgnr(),
            inntektsmeldingFilter.fnr(),
            inntektsmeldingFilter.forespoerselId(),
            inntektsmeldingFilter.ytelseType(),
            inntektsmeldingFilter.fom(),
            inntektsmeldingFilter.tom());

        var dto = inntektsmeldinger.stream().map(InntektsmeldingMapper::mapTilDto).toList();

        return Response.status(Response.Status.OK)
            .entity(dto)
            .build();
    }

    private boolean datoerErUgyldige(InntektsmeldingFilter filterRequest) {
        return filterRequest.fom() != null && filterRequest.tom() != null && filterRequest.fom().isAfter(filterRequest.tom());
    }
}



