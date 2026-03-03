package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

public class AuthKlient {

    private static final Logger LOG = LoggerFactory.getLogger(AuthKlient.class);
    private URI endpoint;

    private static final Environment ENV = Environment.current();

    private static AuthKlient instance;

    protected AuthKlient(URI endpoint) {
        this.endpoint = endpoint;
    }

    public static synchronized AuthKlient instance() {
        var inst = instance;
        if (inst == null) {
            inst = new AuthKlient(ENV.getRequiredProperty("NAIS_TOKEN_INTROSPECTION_ENDPOINT", URI.class));
            instance = inst;
        }
        return inst;
    }

    public TokenIntrospectionResponse introspectToken(TokenString tokenString) {
        TokenValiderRequest tokenValiderRequest = new TokenValiderRequest("maskinporten", tokenString.token());
        var exchangeRequest = HttpRequest.newBuilder()
            .header("Cache-Control", "no-cache")
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(3))
            .uri(endpoint)
            .POST(HttpRequest.BodyPublishers.ofString(DefaultJsonMapper.toJson(tokenValiderRequest)))
            .build();
        return AuthKlient.introspectTokenRetryable(exchangeRequest, 3);
    }

    private static TokenIntrospectionResponse introspectTokenRetryable(HttpRequest request, int retries) {
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

    private static TokenIntrospectionResponse hentToken(HttpRequest request) {
        try (var client = byggHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
            if (response == null || response.body() == null || !responskode2xx(response)) {
                throw new TekniskException("F-157385", "Kunne ikke validere token");
            }
            return DefaultJsonMapper.fromJson(response.body(), TokenIntrospectionResponse.class);
        } catch (IOException e) {
            throw new TekniskException("F-432937", "IOException ved kommunikasjon mot introspection endpoint", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TekniskException("F-432938", "InterruptedException ved introspection av token", e);
        }
    }

    private static boolean responskode2xx(HttpResponse<String> response) {
        var status = response.statusCode();
        return status >= 200 && status < 300;
    }

    private static HttpClient byggHttpClient() {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(2))
            .proxy(HttpClient.Builder.NO_PROXY)
            .build();
    }

    protected record TokenValiderRequest(String identity_provider, String token) {
    }
}
