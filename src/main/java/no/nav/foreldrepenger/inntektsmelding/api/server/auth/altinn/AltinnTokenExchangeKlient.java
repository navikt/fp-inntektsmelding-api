package no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinn;

import java.net.URI;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.server.auth.AuthKlient;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.util.LRUCache;

@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED)
public class AltinnTokenExchangeKlient {
    private static final Logger LOG = LoggerFactory.getLogger(AltinnTokenExchangeKlient.class);
    private static final Environment ENV = Environment.current();
    private static final RestClient restClient = RestClient.client();
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
        return response.body();
    }

    private static boolean responskode2xx(HttpResponse<String> response) {
        var status = response.statusCode();
        return status >= 200 && status < 300;
    }

    public String hentAltinn3Token() {
        String maskinportenToken = hentMaskinportenToken();
        var cacheKey = cacheKey(maskinportenToken);
        var tokenFromCache = getCachedToken(cacheKey);
        if (tokenFromCache != null) {
            LOG.debug("Fant altinn token i cache");
            return tokenFromCache;
        } else {
            LOG.debug("Fant ingen gyldig Altinn token i cache");

            var exchangeRequest = RestRequest.newGET(URI.create(ENV.getRequiredProperty("altinn.tre.token.exchange.path")),
                    RestConfig.forClient(AltinnTokenExchangeKlient.class))
                .header("Cache-Control", "no-cache")
                .header("Authorization", "Bearer " + maskinportenToken)
                .timeout(Duration.ofSeconds(3));

            var token = hentTokenRetryable(exchangeRequest, 3);
            putTokenToCache(cacheKey, token);
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

    private String getCachedToken(String key) {
        return altinnCache.get(key);
    }

    private void putTokenToCache(String key, String exchangedToken) {
        altinnCache.put(key, exchangedToken);
    }

    private String cacheKey(String maskinportenToken) {
        try {
            var md = MessageDigest.getInstance("MD5");
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
            throw new TekniskException("PKI-845346", "MD5 algoritme finnes ikke", e);
        }
    }

    protected record MaskinportenTokenRequest(String identity_provider, String target) {
    }

    protected record MaskinportenTokenResponse(String access_token) {
    }
}
