package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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



    public FpinntektsmeldingKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(FpinntektsmeldingKlient.class);
        this.uriHentForespørsel = toUri(restConfig.fpContextPath(), "/api/foresporsel-ekstern/hent");
    }

    public Forespørsel hentForespørsel(UUID forespørselUuid) {
        Objects.requireNonNull(forespørselUuid);
        try {
            LOG.info("Sender request til fpinntektsmelding for forespørselUuid {} ", forespørselUuid);
            var request = RestRequest.newGET(toUri(uriHentForespørsel, "/" + forespørselUuid), restConfig);
           return restClient.send(request, Forespørsel.class);
        } catch (Exception e) {
            LOG.warn("Feil ved henting av forespørsel fra fpinntektsmelding for uuid: {}", forespørselUuid);
            throw feilVedKallTilFpinntektsmelding(e.getMessage());
        }
    }

    private static TekniskException feilVedKallTilFpinntektsmelding(String feilmelding) {
        return new TekniskException("FP-97215", "Feil ved kall til Fpinntektsmelding: " + feilmelding);
    }

    private URI toUri(URI endpointURI, String path) {
        try {
            return UriBuilder.fromUri(endpointURI).path(path).build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ugyldig uri: " + endpointURI + path, e);
        }
    }
}

