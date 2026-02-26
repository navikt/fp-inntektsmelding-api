package no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinn;

import java.net.URI;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.foreldrepenger.inntektsmelding.api.server.auth.AuthKlient;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.util.LRUCache;

@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED, endpointProperty = "altinn.tre.token.exchange.path", endpointDefault = "https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten")
public class AltinnTokenExchangeKlient {
    private static final Logger LOG = LoggerFactory.getLogger(AltinnTokenExchangeKlient.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");
    private static final Environment ENV = Environment.current();
    private static final RestClient restClient = RestClient.client();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static AltinnTokenExchangeKlient instance;
    private final LRUCache<String, String> altinnCache;

    private AltinnTokenExchangeKlient() {
        this.altinnCache = new LRUCache<>(2, TimeUnit.MILLISECONDS.convert(29, TimeUnit.MINUTES));
    }

    public static synchronized AltinnTokenExchangeKlient instance() {
        var inst = instance;
        if (inst == null) {
            inst = new AltinnTokenExchangeKlient();
            instance = inst;
        }
        return inst;
    }

    private static String hentTokenRetryable(RestRequest request, int retries) {
        int i = retries;
        while (i-- > 0) {
            try {
                return hentToken(request);
            } catch (TekniskException e) {
                LOG.info("Feilet {}. gang ved henting av token. Prøver på nytt", retries - i, e);
            }
        }
        return hentToken(request);
    }

    private static String hentToken(RestRequest request) {
        var response = restClient.sendReturnUnhandled(request);
        if (response == null || response.body() == null || !responskode2xx(response)) {
            throw new TekniskException("F-157385", "Kunne ikke hente token");
        }
        // Altinn returns the token as a JSON string literal, so we deserialize it with Jackson
        return DefaultJsonMapper.fromJson(response.body(), String.class);
    }

    private static boolean responskode2xx(HttpResponse<String> response) {
        var status = response.statusCode();
        return status >= 200 && status < 300;
    }

    public String hentAltinn3Token() {
        String maskinportenToken = hentMaskinportenToken();
        var altinnTokenCacheKey = cacheKey(maskinportenToken);
        var altinnTokenFromCache = getCachedAltinnToken(altinnTokenCacheKey);
        if (altinnTokenFromCache != null) {
            LOG.debug("Fant altinn token i cache.");
            SECURE_LOG.debug("altinntoken: {}", altinnTokenFromCache);
            return altinnTokenFromCache;
        } else {
            LOG.debug("Fant ingen gyldig Altinn token i cache.");

            var exchangeRequest = RestRequest.newGET(URI.create(ENV.getRequiredProperty("altinn.tre.base.url") + ENV.getRequiredProperty("altinn.tre.token.exchange.path")),
                    RestConfig.forClient(AltinnTokenExchangeKlient.class))
                .header("Cache-Control", "no-cache")
                .header("Authorization", "Bearer " + maskinportenToken)
                .timeout(Duration.ofSeconds(3));

            var token = hentTokenRetryable(exchangeRequest, 3);
            putTokenToCache(altinnTokenCacheKey, token);
            SECURE_LOG.debug("altinntoken:{}", token);
            return token;
        }
    }

    private String hentMaskinportenToken() {
        String endpoint = ENV.getRequiredProperty("NAIS_TOKEN_ENDPOINT");
        AltinnTokenExchangeKlient.MaskinportenTokenRequest tokenRequest = new MaskinportenTokenRequest("maskinporten",
            "altinn:authorization/authorize");
        RestRequest postRequest = RestRequest.newPOSTJson(tokenRequest, URI.create(endpoint), RestConfig.forClient(AuthKlient.class));
        return restClient.send(postRequest, MaskinportenTokenResponse.class).access_token();
    }

    private String getCachedAltinnToken(String key) {
        return altinnCache.get(key);
    }

    private void putTokenToCache(String key, String exchangedToken) {
        altinnCache.put(key, exchangedToken);
    }

    private String cacheKey(String maskinportenToken) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = maskinportenToken.getBytes();
            byte[] hash = md.digest(keyBytes);
            var hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            md.reset();
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new TekniskException("PKI-845346", "SHA-256 algoritme finnes ikke", e);
        }
    }

    protected record MaskinportenTokenRequest(String identity_provider, String target) {
    }

    protected record MaskinportenTokenResponse(String access_token) {
    }
}
