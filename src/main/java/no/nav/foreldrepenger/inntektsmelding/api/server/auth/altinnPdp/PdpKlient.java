package no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinnPdp;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinn.AltinnTokenExchangeKlient;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED, endpointProperty = "altinn.tre.base.url", endpointDefault = "https://platform.altinn.no")
public class PdpKlient {
    private static final Environment ENV = Environment.current();
    private static final Logger logger = LoggerFactory.getLogger(PdpKlient.class);
    private static final Logger secureLogger = LoggerFactory.getLogger("secureLogger");

    private final String baseUrl;
    private final String subscriptionKey;
    private static PdpKlient instance;
    private final AltinnTokenExchangeKlient altinnTokenExchangeKlient;


    private PdpKlient() {
        this.baseUrl = ENV.getRequiredProperty("altinn.tre.base.url");
        this.subscriptionKey = ENV.getRequiredProperty("ALTINN_TRE_SUBSCRIPTION_KEY");
        this.altinnTokenExchangeKlient = AltinnTokenExchangeKlient.instance();
    }

    public static synchronized PdpKlient instance() {
        var inst = instance;
        if (inst == null) {
            inst = new PdpKlient();
            instance = inst;
        }
        return inst;
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
/*            RestRequest request = RestRequest.newPOSTJson(pdpRequest,
                    URI.create(baseUrl + "/authorization/api/v1/authorize"),
                    RestConfig.forClient(PdpKlient.class))
                .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .otherAuthorizationSupplier(altinnTokenExchangeKlient::hentAltinn3Token);*/

            var httpPost = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + altinnTokenExchangeKlient.hentAltinn3Token())
                .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .timeout(Duration.ofSeconds(3))
                .uri(URI.create(baseUrl + "/authorization/api/v1/authorize"))
                .POST(HttpRequest.BodyPublishers.ofString(DefaultJsonMapper.toJson(pdpRequest)))
                .build();

            var pdpResponse = authorize(httpPost);

            if (ENV.isProd()) {
                secureLogger.debug("PDP respons: {}", pdpResponse);
            } else {
                logger.info("PDP respons: {}", pdpResponse);
            }
            return DefaultJsonMapper.fromJson(pdpResponse, PdpResponse.class);
        } catch (Exception e) {
            String message = "Feil ved kall til pdp endepunkt";
            if (ENV.isProd()) {
                logger.error(message);
                secureLogger.error(message, e);
            } else {
                logger.error(message, e);
            }
            throw new PdpClientException();
        }
    }

    private static String authorize(HttpRequest request) {
        try (var client = byggHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
            if (response == null || response.body() == null || !responskode2xx(response)) {
                throw new TekniskException("F-157385", "Kunne ikke authorisete kall til pdp endepunkt, status: " + (response != null ? response.statusCode() : "null"));
            }
            return response.body();
        } catch (IOException e) {
            throw new TekniskException("F-432937", "IOException ved kommunikasjon med server", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TekniskException("F-432938", "InterruptedException ved henting av token", e);
        }
    }

    private static boolean responskode2xx(HttpResponse<String> response) {
        var status = response.statusCode();
        return status >= 200 && status < 300;
    }

    private static HttpClient byggHttpClient() {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(3))
            .proxy(HttpClient.Builder.NO_PROXY)
            .build();
    }

    public record System(String id, String attributeId) {
    }

    //Todo: burde vi her opprette en InntektsmeldingAPIException med en EksponertFeilmelding som kan brukes i TilgangTjeneste istedenfor en egen PdpClientException?
    static class PdpClientException extends Exception {
        public PdpClientException() {
            super("Feil ved kall til pdp endepunkt");
        }
    }
}
