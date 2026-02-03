package no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinnPdp;

import no.nav.vedtak.klient.http.DefaultHttpClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.Set;

public class PdpKlient {
    private static final Logger logger = LoggerFactory.getLogger(no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinnPdp.PdpKlient.class);
    private static final Logger sikkerLogger = LoggerFactory.getLogger("sikkerLogger");

    private final String baseUrl;
    private final String subscriptionKey;
    private final DefaultHttpClient httpClient;

    public PdpKlient(String baseUrl, String subscriptionKey) {
        this.baseUrl = baseUrl;
        this.subscriptionKey = subscriptionKey;
        this.httpClient = DefaultHttpClient.client();
    }

    public boolean systemHarRettighetForOrganisasjoner(String systembrukerId, Set<String> orgnumre, String ressurs) throws Exception {
        return pdpKallMulti(new System(systembrukerId, "urn:altinn:systemuser:uuid"), orgnumre, Set.of(ressurs)).harTilgang();
    }

    public boolean systemHarRettighetForOrganisasjonerForRessurser(String systembrukerId,
                                                                   Set<String> orgnumre,
                                                                   Set<String> ressurser) throws Exception {
        return pdpKallMulti(new System(systembrukerId,"urn:altinn:systemuser:uuid"), orgnumre, ressurser).harTilgang();
    }

    private PdpResponse pdpKallMulti(System system, Set<String> orgnumre, Set<String> ressurser) throws PdpClientException {
        if (orgnumre.isEmpty()) {
            String message = "Ingen organisasjonsnumre gitt for pdp-kall";
            logger.warn(message);
            sikkerLogger.warn(message);
            throw new IllegalArgumentException(message);
        }

        if (ressurser.isEmpty()) {
            String message = "Ingen ressurser gitt for pdp-kall";
            logger.warn(message);
            sikkerLogger.warn(message);
            throw new IllegalArgumentException(message);
        }

        PdpRequest pdpRequest = lagPdpMultiRequest(system, orgnumre, ressurser);
        sikkerLogger.debug("PDP kall for {}: {}", ressurser, pdpRequest);

        try {
            PdpResponse response = httpClient
                .post(baseUrl + "/authorization/api/v1/authorize")
                .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(pdpRequest)
                .execute();

            String raw = response.getRaw();
            sikkerLogger.debug("Raw PDP respons: {}", raw);
            sikkerLogger.debug("PDP respons: {}", response);
            return response;
        } catch (Exception e) {
            String message = "Feil ved kall til pdp endepunkt";
            logger.error(message);
            sikkerLogger.error(message, e);
            throw new PdpClientException();
        }
    }

    class PdpClientException extends Exception {
        public PdpClientException() {
            super("Feil ved kall til pdp endepunkt");
        }
    }

    record System(String id, String attributeId) {}
}
