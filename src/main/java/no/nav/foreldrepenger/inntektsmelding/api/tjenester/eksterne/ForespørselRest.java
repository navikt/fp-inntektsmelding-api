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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.api.integrasjoner.FpinntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.api.server.auth.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.foreldrepenger.inntektsmelding.api.typer.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Path(ForespørselRest.BASE_PATH)
@Tag(name = "Forespørsel om inntektsmelding")
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
    @Operation(summary = "Hent forespørsel", description = "Henter en spesifikk forespørsel om inntektsmelding basert på forespørselens UUID.")
    @ApiResponse(responseCode = "200", description = "Forespørselen ble funnet",
        content = @Content(schema = @Schema(implementation = ForespørselDto.class)))
    @ApiResponse(responseCode = "400", description = "Ugyldig UUID-format",
        content = @Content(schema = @Schema(implementation = no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse.class),
            examples = @ExampleObject(value = "{\"feilkode\":\"SERIALISERINGSFEIL\",\"feilmelding\":\"Serialiseringsfeil: ...\",\"feilreferanse\":\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\"}")))
    @ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering",
        content = @Content(schema = @Schema(implementation = no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse.class),
            examples = @ExampleObject(value = "{\"feilkode\":\"MANGLER_TOKEN\",\"feilmelding\":\"Mangler token i header\",\"feilreferanse\":null}")))
    @ApiResponse(responseCode = "403", description = "Ikke tilgang til oppgitt organisasjon")
    @ApiResponse(responseCode = "404", description = "Forespørselen ble ikke funnet",
        content = @Content(schema = @Schema(implementation = no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse.class),
            examples = @ExampleObject(value = "{\"feilkode\":\"TOM_FORESPOERSEL\",\"feilmelding\":\"Finner ikke forespørsel: 3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"feilreferanse\":null}")))
    @ApiResponse(responseCode = "500", description = "Intern serverfeil",
        content = @Content(schema = @Schema(implementation = no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse.class),
            examples = @ExampleObject(value = "{\"feilkode\":\"STANDARD_FEIL\",\"feilmelding\":\"Noe feilet.\",\"feilreferanse\":null}")))
    public Response hentForespørsel(@NotNull @Valid @PathParam("uuid")
                                    @Parameter(description = "UUID til forespørselen")
                                    @Pattern(regexp = "^[a-fA-F\\d]{8}(?:-[a-fA-F\\d]{4}){3}-[a-fA-F\\d]{12}$", message = "Ugyldig UUID-format") String forespørselUuid) {
        LOG.info("Innkomende kall på hent forespørsel {}", forespørselUuid);
        var uuid = UUID.fromString(forespørselUuid);

        Forespørsel forespørsel = fpinntektsmeldingTjeneste.hentForespørsel(uuid);
        if (forespørsel == null) {
            return Response.status(Response.Status.NOT_FOUND).
                entity(new ErrorResponse(EksponertFeilmelding.TOM_FORESPOERSEL.name(), EksponertFeilmelding.TOM_FORESPOERSEL.getTekst() + ": " + forespørselUuid,
                    forespørselUuid)).build();
        }

        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(forespørsel.orgnummer());
        var dto = mapTilDto(forespørsel);
        return Response.ok(dto).build();
    }


    @POST
    @Path(HENT_FLERE)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Hent forespørsler", description = "Filtrer forespørsler om inntektsmelding på orgnr, fnr, forespørselId, status, ytelseType og/eller dato forespørselen ble opprettet av NAV.")
    @ApiResponse(responseCode = "200", description = "Liste med forespørsler som matcher filteret",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ForespørselDto.class))))
    @ApiResponse(responseCode = "400", description = "Ugyldig periode (fom er etter tom)",
        content = @Content(schema = @Schema(implementation = no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse.class),
            examples = @ExampleObject(value = "{\"feilkode\":\"UGYLDIG_PERIODE\",\"feilmelding\":\"Oppgitt periode er ugyldig, fom kan ikke være etter tom\",\"feilreferanse\":null}")))
    @ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering",
        content = @Content(schema = @Schema(implementation = no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse.class),
            examples = @ExampleObject(value = "{\"feilkode\":\"MANGLER_TOKEN\",\"feilmelding\":\"Mangler token i header\",\"feilreferanse\":null}")))
    @ApiResponse(responseCode = "403", description = "Ikke tilgang til oppgitt organisasjon")
    @ApiResponse(responseCode = "500", description = "Intern serverfeil",
        content = @Content(schema = @Schema(implementation = no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse.class),
            examples = @ExampleObject(value = "{\"feilkode\":\"STANDARD_FEIL\",\"feilmelding\":\"Noe feilet.\",\"feilreferanse\":null}")))
    public Response hentForespørsler(@NotNull @Valid ForespørselFilter filterRequest) {
        LOG.info("Innkomende kall på søk etter forespørsler");

        // Det er spurt etter en spesifikk forespørsel, henter kun denne
        if (filterRequest.forespoerselId() != null) {
            Forespørsel forespørsel = fpinntektsmeldingTjeneste.hentForespørsel(filterRequest.forespoerselId());
            if (forespørsel == null) {
                return Response.ok(List.of()).build();
            }
            tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(forespørsel.orgnummer());
            return Response.ok(List.of(mapTilDto(forespørsel))).build();
        }

        if (datoerErUgyldige(filterRequest)) {
            return Response.status(Response.Status.BAD_REQUEST)
                 .entity(new ErrorResponse(EksponertFeilmelding.UGYLDIG_PERIODE.name(), EksponertFeilmelding.UGYLDIG_PERIODE.getTekst()))
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
