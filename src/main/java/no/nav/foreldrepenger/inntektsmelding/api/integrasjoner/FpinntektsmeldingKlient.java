package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uriHentForespørsel;
    private final URI uriSendInntektsmelding;
    private final URI uriHentForespørsler;

    public FpinntektsmeldingKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(FpinntektsmeldingKlient.class);
        this.uriHentForespørsel = toUri(restConfig.fpContextPath(), "/imapi/foresporsel/hent");
        this.uriHentForespørsler = toUri(restConfig.fpContextPath(), "/imapi/foresporsel/foresporsler");
        this.uriSendInntektsmelding = toUri(restConfig.fpContextPath(), "/imapi/inntektsmelding/send-inntektsmelding");
    }

    public ForespørselResponse hentForespørsel(UUID forespørselUuid) {
        try {
            LOG.info("Sender request til fpinntektsmelding for forespørselUuid {} ", forespørselUuid);
            var request = RestRequest.newGET(toUri(uriHentForespørsel, "/" + forespørselUuid), restConfig);
            return restClient.send(request, ForespørselResponse.class);
        } catch (Exception e) {
            LOG.warn("FP-97215: Feil ved henting av forespørsel fra fpinntektsmelding for uuid: {}. Feilmelding var {}",
                forespørselUuid,
                e.getMessage());
            throw feilVedKallTilFpinntektsmelding();
        }
    }

    public List<ForespørselResponse> hentForespørsler(ForespørselFilterRequest filter) {
        try {
            var request = RestRequest.newPOSTJson(filter, uriHentForespørsler, restConfig);
            var response = restClient.send(request, ForespørselResponse[].class);
            return List.of(response);
        } catch (Exception e) {
            LOG.warn("FP-97215: Feil ved henting av forespørsler fra fpinntektsmelding for orgnr: {}. Feilmelding var {}",
                filter.orgnr(),
                e.getMessage());
            throw feilVedKallTilFpinntektsmelding();
        }
    }

    Response sendInntektsmelding(InntektsmeldingRequestDto inntektsmeldingRequestDto) {
        try {
            LOG.info("Sender inntektsmelding til fpinntektsmelding for forespørselUuid {} ", inntektsmeldingRequestDto.forespørselUuid());
            var request = RestRequest.newPOSTJson(inntektsmeldingRequestDto, uriSendInntektsmelding, restConfig);
            return restClient.send(request, Response.class);
        } catch (Exception e) {
            LOG.warn("FP-97215: Feil ved sending av inntektsmelding-api til fpinntektsmelding for uuid: {}. Feilmelding var {}", inntektsmeldingRequestDto.forespørselUuid(), e.getMessage());
            SECURE_LOG.info("FP-97215: Feil ved sending av inntektsmelding-api til fpinntektsmelding. InntektsmeldingRequestDto er {}", inntektsmeldingRequestDto);
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
}

