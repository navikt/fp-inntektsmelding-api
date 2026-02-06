package no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinnPdp;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinn.AltinnTokenExchangeKlient;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;

@ApplicationScoped
public class PdpKlient {
    private static final Logger logger = LoggerFactory.getLogger(PdpKlient.class);
    private static final Logger secureLogger = LoggerFactory.getLogger("secureLogger");

    private final String baseUrl;
    private final String subscriptionKey;
    private final RestClient restClient;
    private final AltinnTokenExchangeKlient altinnTokenExchangeKlient;

    // TODO finn subscription key
    @Inject
    public PdpKlient(String baseUrl, String subscriptionKey) {
        this.baseUrl = baseUrl;
        this.subscriptionKey = subscriptionKey;
        this.altinnTokenExchangeKlient = AltinnTokenExchangeKlient.instance();
        this.restClient = RestClient.client();
    }

    public boolean systemHarRettighetForOrganisasjon(String systembrukerId, String orgnummer, String ressurs) throws Exception {
        return pdpKall(new System(systembrukerId, "urn:altinn:systemuser:uuid"), orgnummer, ressurs).harTilgang();
    }

    private PdpResponse pdpKall(System system, String orgnummer, String ressurs) throws PdpClientException {
        if (orgnummer == null) {
            String message = "Ingen organisasjonsnumre gitt for pdp-kall";
            logger.warn(message);
            secureLogger.warn(message);
            throw new IllegalArgumentException(message);
        }

        if (ressurs == null) {
            String message = "Ingen ressurser gitt for pdp-kall";
            logger.warn(message);
            secureLogger.warn(message);
            throw new IllegalArgumentException(message);
        }

        PdpRequest pdpRequest = PdpRequestUtil.lagPdpRequest(system, orgnummer, ressurs);
        secureLogger.debug("PDP kall for {}: {}", ressurs, pdpRequest);

        try {
            RestRequest request = RestRequest.newPOSTJson(pdpRequest,
                    URI.create(baseUrl + "/authorization/api/v1/authorize"),
                    RestConfig.forClient(PdpKlient.class))
                .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .otherAuthorizationSupplier(altinnTokenExchangeKlient::hentAltinn3Token);

            var pdpResponse = restClient.send(request, PdpResponse.class);

            secureLogger.debug("PDP respons: {}", pdpResponse);
            return pdpResponse;
        } catch (Exception e) {
            String message = "Feil ved kall til pdp endepunkt";
            logger.error(message);
            secureLogger.error(message, e);
            throw new PdpClientException();
        }
    }

    // ID = systemUserId
    // attributeId = urn:altinn:systemuser:uuid
    public record System(String id, String attributeId) {
    }

    class PdpClientException extends Exception {
        public PdpClientException() {
            super("Feil ved kall til pdp endepunkt");
        }
    }
}
