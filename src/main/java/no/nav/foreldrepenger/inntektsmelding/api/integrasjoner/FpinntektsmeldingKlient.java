package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.api.typer.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.OrganisasjonsnummerDto;

import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.InntektsmeldingAPIException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPINNTEKTSMELDING)
public class FpinntektsmeldingKlient {
    private static final Logger LOG = LoggerFactory.getLogger(FpinntektsmeldingKlient.class);

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uriHentForespørsel;
    private final URI uriHentForespørsler;

    public FpinntektsmeldingKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(FpinntektsmeldingKlient.class);
        this.uriHentForespørsel = toUri(restConfig.fpContextPath(), "/api/foresporsel-ekstern/hent");
        this.uriHentForespørsler = toUri(restConfig.fpContextPath(), "api/foresporsel-ekstern/hent-foresporsler");
    }

    public Forespørsel hentForespørsel(UUID forespørselUuid) {
        try {
            LOG.info("Sender request til fpinntektsmelding for forespørselUuid {} ", forespørselUuid);
            var request = RestRequest.newGET(toUri(uriHentForespørsel, "/" + forespørselUuid), restConfig);
            var response = restClient.send(request, ForespørselResponse.class);
            return mapResponseTilDomeneobjekt(response);
        } catch (Exception e) {
            LOG.warn("FP-97215: Feil ved henting av forespørsel fra fpinntektsmelding for uuid: {}. Feilmelding var {}",
                forespørselUuid,
                e.getMessage());
            throw feilVedKallTilFpinntektsmelding();
        }
    }

    public List<Forespørsel> hentForespørsler(OrganisasjonsnummerDto organisasjonsnummerDto,
                                              FødselsnummerDto fødselsnummerDto,
                                              ForespørselStatus forespørselStatus,
                                              LocalDate fom,
                                              LocalDate tom) {
        try {
            LOG.info("Sender request til fpinntektsmelding for å hente filtrerte forespørsler for orgnr {} ", organisasjonsnummerDto);
            var filter = new ForespørselFilterRequest(organisasjonsnummerDto, fødselsnummerDto, forespørselStatus, fom, tom);
            var request = RestRequest.newPOSTJson(filter, uriHentForespørsler, restConfig);
            var response = restClient.send(request, ForespørselResponse[].class);
            return Arrays.stream(response).map(this::mapResponseTilDomeneobjekt).toList();
        } catch (Exception e) {
            LOG.warn("FP-97215: Feil ved henting av forespørsler fra fpinntektsmelding for orgnr: {}. Feilmelding var {}",
                organisasjonsnummerDto,
                e.getMessage());
            throw feilVedKallTilFpinntektsmelding();
        }
    }

    private static TekniskException feilVedKallTilFpinntektsmelding() {
        throw new InntektsmeldingAPIException(EksponertFeilmelding.STANDARD_FEIL, Response.Status.INTERNAL_SERVER_ERROR);
    }

    private URI toUri(URI endpointURI, String path) {
        try {
            return UriBuilder.fromUri(endpointURI).path(path).build();
        } catch (Exception e) {
            LOG.warn("Ugyldig uri: {}, feilmelding {}", endpointURI + path, e.getMessage());
            throw new InntektsmeldingAPIException(EksponertFeilmelding.STANDARD_FEIL, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Forespørsel mapResponseTilDomeneobjekt(ForespørselResponse response) {
        return new Forespørsel(response.forespørselUuid(),
            response.orgnummer(),
            response.fødselsnummer(),
            response.førsteUttaksdato(),
            response.skjæringstidspunkt(),
            response.status(),
            response.ytelseType());
    }

    protected record ForespørselFilterRequest(OrganisasjonsnummerDto orgnr, FødselsnummerDto fnr,
                                              ForespørselStatus status, LocalDate fom, LocalDate tom) {
    }

    public record ForespørselResponse(UUID forespørselUuid, OrganisasjonsnummerDto orgnummer, String fødselsnummer, LocalDate førsteUttaksdato,
                                      LocalDate skjæringstidspunkt, ForespørselStatus status, YtelseType ytelseType) {
    }

}

