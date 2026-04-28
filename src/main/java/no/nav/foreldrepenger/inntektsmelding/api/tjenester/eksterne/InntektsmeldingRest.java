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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.foreldrepenger.inntektsmelding.api.inntektsmelding.InntektsmeldingMapper;
import no.nav.foreldrepenger.inntektsmelding.api.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;

import no.nav.foreldrepenger.inntektsmelding.felles.FeilkodeDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.integrasjoner.FpinntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.api.server.auth.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import java.util.UUID;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(InntektsmeldingRest.BASE_PATH)
@Tag(name = "Inntektsmelding")
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
    @Operation(summary = "Send inn inntektsmelding",
        description = "Sender inn en inntektsmelding for en gitt forespørsel. Inntekten valideres mot A-inntekt og duplikater avvises.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inntektsmeldingen ble mottatt. Returnerer UUID til den innsendte inntektsmeldingen.",
            content = @Content(schema = @Schema(implementation = java.util.UUID.class))),
        @ApiResponse(responseCode = "400", description = "Valideringsfeil eller ugyldig inntektsmelding (f.eks. inntekt avviker fra A-inntekt uten endringsårsak)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"MISMATCH_ORGNR","feilmelding":"Organisasjonsnummer fra token og organisasjonsnummer fra etterspurt forespørsel matcher ikke","feilreferanseId":"3fa85f64-5717-4562-b3fc-2c963f66afa6"}"""))),
        @ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"MANGLER_TOKEN","feilmelding":"Mangler token i header","feilreferanseId":null}"""))),
        @ApiResponse(responseCode = "403", description = "Ikke tilgang til oppgitt organisasjon"),
        @ApiResponse(responseCode = "404", description = "Forespørselen ble ikke funnet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"TOM_FORESPOERSEL","feilmelding":"Finner ikke forespørsel: 3fa85f64-5717-4562-b3fc-2c963f66afa6","feilreferanseId":null}"""))),
        @ApiResponse(responseCode = "409", description = "Duplikat – inntektsmelding er identisk med siste innsendte",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"DUPLIKAT","feilmelding":"Inntektsmelding avvises. Ingen endring på ny inntektsmelding sammenlignet med tidligere innsendt inntektsmelding","feilreferanseId":"H184i1D5UNPxL7Pn"}"""))),
        @ApiResponse(responseCode = "503", description = "A-inntekt er midlertidig utilgjengelig. Prøv igjen om litt.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"NEDETID_AINNTEKT","feilmelding":"Inntektskomponenten har nedetid, og vi kan ikke verifisere inntekt i inntektsmeldingen mot A-inntekt. Prøv igjen om litt.","feilreferanseId":"H184i1D5UNPxL7Pn"}"""))),
        @ApiResponse(responseCode = "500", description = "Intern serverfeil",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"STANDARD_FEIL","feilmelding":"Noe feilet.","feilreferanseId":"H184i1D5UNPxL7Pn"}""")))
    })
    public Response sendInntektsmelding(@Valid @NotNull InntektsmeldingRequest inntektsmeldingRequest) {
        var forespørselUuid = inntektsmeldingRequest.foresporselUuid();
        LOG.info("Mottatt inntektsmelding for forespørselUuid {} ", forespørselUuid);
        var forespørsel = fpinntektsmeldingTjeneste.hentForespørsel(forespørselUuid);

        if (forespørsel == null) {
            LOG.info("Avvist inntektsmelding for forespørselUuid {}. Forespørsel ikke funnet.", forespørselUuid);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(EksponertFeilmelding.TOM_FORESPOERSEL.name(),
                    EksponertFeilmelding.TOM_FORESPOERSEL.getTekst() + ": " + forespørselUuid,
                    forespørselUuid.toString()))
                .build();
        }

        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(forespørsel.orgnummer().orgnr()));

        var feilmelding = InntektsmeldingValidererUtil.validerInntektsmelding(inntektsmeldingRequest, forespørsel);
        if (feilmelding.isPresent()) {
            LOG.info("Avvist inntektsmelding for forespørselUuid {}. Validering av inntektsmelding feilet. Feilmelding: {}",
                inntektsmeldingRequest.foresporselUuid(), feilmelding.get().getTekst());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(feilmelding.get().name(), feilmelding.get().getTekst(), forespørselUuid.toString()))
                .build();
        }
        var response = fpinntektsmeldingTjeneste.sendInntektsmelding(inntektsmeldingRequest, forespørsel);

        if (response.success()) {
            return Response.ok(response.inntektsmeldingUuid()).build();
        } else {
            var errorResponse = new ErrorResponse(response.feilinformasjon().feilkode().name(),
                response.feilinformasjon().feilmelding(),
                response.feilinformasjon().referanseId());

            if (FeilkodeDto.DUPLIKAT.equals(response.feilinformasjon().feilkode())) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(errorResponse)
                    .build();
            } else if (FeilkodeDto.NEDETID_AINNTEKT.equals(response.feilinformasjon().feilkode())) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(errorResponse)
                    .build();
            } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
            }
        }
    }

    @GET
    @Path(HENT_INNTEKTSMELDING)
    @Operation(summary = "Hent inntektsmelding", description = "Henter en spesifikk inntektsmelding basert på innsendingsUUID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inntektsmeldingen ble funnet",
            content = @Content(schema = @Schema(implementation = InntektsmeldingDto.class))),
        @ApiResponse(responseCode = "400", description = "Ugyldig UUID-format",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"SERIALISERINGSFEIL","feilmelding":"Serialiseringsfeil: ...","feilreferanseId":"H184i1D5UNPxL7Pn"}"""))),
        @ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"MANGLER_TOKEN","feilmelding":"Mangler token i header","feilreferanseId":null}"""))),
        @ApiResponse(responseCode = "403", description = "Ikke tilgang til oppgitt organisasjon"),
        @ApiResponse(responseCode = "404", description = "Inntektsmeldingen ble ikke funnet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"TOM_INNTEKTSMELDING","feilmelding":"Finner ikke inntektsmelding: 3fa85f64-5717-4562-b3fc-2c963f66afa6","feilreferanseId":"H184i1D5UNPxL7Pn"}"""))),
        @ApiResponse(responseCode = "500", description = "Intern serverfeil",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"STANDARD_FEIL","feilmelding":"Noe feilet.","feilreferanseId":"H184i1D5UNPxL7Pn"}""")))
    })
    public Response hentInntektsmelding(@NotNull @Valid @PathParam("uuid")
                                        @Parameter(description = "UUID til inntektsmeldingen (innsendingId)")
                                        @Pattern(regexp = "^[a-fA-F\\d]{8}(?:-[a-fA-F\\d]{4}){3}-[a-fA-F\\d]{12}$", message = "Ugyldig UUID-format")
                                        String innsendingId) {
        LOG.info("Hent inntektsmelding med innsendingId {} ", innsendingId);
        var inntektsmelding = fpinntektsmeldingTjeneste.hentInntektsmelding(UUID.fromString(innsendingId));

        if (inntektsmelding == null) {
            LOG.info("Avvist inntektsmelding for innsendingId {}. Inntektsmelding ikke funnet.", innsendingId);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(EksponertFeilmelding.TOM_INNTEKTSMELDING.name(),
                    EksponertFeilmelding.TOM_INNTEKTSMELDING.getTekst() + ": " + innsendingId,
                    innsendingId))
                .build();
        }

        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(inntektsmelding.orgnr().orgnr()));

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        return Response.status(Response.Status.OK)
            .entity(dto)
            .build();
    }

    @POST
    @Path(HENT_INNTEKTSMELDINGER)
    @Operation(summary = "Hent inntektsmeldinger", description = "Filtrer inntektsmeldinger på orgnr, fnr, forespørselId, innsendingId, ytelseType og/eller dato inntektsmeldingen ble mottatt av NAV.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste med inntektsmeldinger som matcher filteret",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = InntektsmeldingDto.class)))),
        @ApiResponse(responseCode = "400", description = "Ugyldig periode (fom er etter tom)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"UGYLDIG_PERIODE","feilmelding":"Oppgitt periode er ugyldig, fom kan ikke være etter tom","feilreferanseId":null}"""))),
        @ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"MANGLER_TOKEN","feilmelding":"Mangler token i header","feilreferanseId":null}"""))),
        @ApiResponse(responseCode = "403", description = "Ikke tilgang til oppgitt organisasjon"),
        @ApiResponse(responseCode = "500", description = "Intern serverfeil",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"feilkode":"STANDARD_FEIL","feilmelding":"Noe feilet.","feilreferanseId":"H184i1D5UNPxL7Pn"}""")))
    })
    public Response hentInntektsmeldinger(@NotNull @Valid InntektsmeldingFilter inntektsmeldingFilter) {
        LOG.info("Innkomende kall på søk etter inntektsmeldinger");
        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(inntektsmeldingFilter.orgnr()));
        if (inntektsmeldingFilter.innsendingId() != null) {
            var inntektsmelding = fpinntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingFilter.innsendingId());
            if (inntektsmelding == null) {
                LOG.info("Inntektsmelding med innsendingId {} ikke funnet.", inntektsmeldingFilter.innsendingId());
                return Response.ok(new ErrorResponse(EksponertFeilmelding.TOM_INNTEKTSMELDING.name(), EksponertFeilmelding.TOM_INNTEKTSMELDING.getTekst(), inntektsmeldingFilter.innsendingId().toString())).build();
            }
            if (datoerErUgyldige(inntektsmeldingFilter)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(EksponertFeilmelding.UGYLDIG_PERIODE.name(), EksponertFeilmelding.UGYLDIG_PERIODE.getTekst()))
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



