package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED)
public class AuthKlient  {
    private static final Logger LOG = LoggerFactory.getLogger(AuthKlient.class);

    private final RestClient restClient;
    private static AuthKlient instance = new AuthKlient();
    private static final Environment ENV = Environment.current();
    private AuthKlient() {
        this(RestClient.client());
    }

    AuthKlient(RestClient restClient) {
        this.restClient = restClient;
    }

    public static synchronized AuthKlient instance() {
        var inst = instance;
        if (inst == null) {
            inst = new AuthKlient();
            instance = inst;
        }
        return inst;
    }

    public void valider(TokenString tokenString) {

        // Autentisering - valider token
        String endpoint = ENV.getRequiredProperty("NAIS_TOKEN_INTROSPECTION_ENDPOINT");
        TokenValiderRequest tokeValiderRequest = new TokenValiderRequest("maskinporten", tokenString.token());
        RestRequest postRequest = RestRequest.newPOSTJson(tokeValiderRequest, URI.create(endpoint), RestConfig.forClient(AuthKlient.class));
        TokenIntrospectionResponse response = restClient.send(postRequest, TokenIntrospectionResponse.class);
        if (!response.active) {
            // FEIL
        }

        // TODO validere at dette er rett authentication level
        // var authenticationLevel = Set.of("Level3", "idporten-loa-substantial"); // Level4 er gammel og utgår ila 2023

        // Autentisering - valider scopes
        List<String> scopes = List.of(response.scope().split(" "));
        var gyldigScope = "nav:inntektsmelding/foreldrepenger";
        boolean harGyldigScope = scopes.stream().anyMatch(s -> s.equals(gyldigScope));
        if (!harGyldigScope) {
            // FEIL
        }
        // TODO sett TokenKontekst med info fra tokenet
        var tokenKontekst = new TokenKontekst(
            response.consumer.ID,
            response.consumer.ID,
            response.authorization_details.systemuser_org.ID);

        KontekstHolder.setKontekst(tokenKontekst);

        // Autorisering - valider altinn tilganger
        String altinn3Token = hentAltinn3Token();
        var orgnummer = response.authorization_details.systemuser_org.ID;
        if (orgnummer == null || orgnummer.isBlank()) {
            // FEIL
        }
    }

    private String hentAltinn3Token() {
        String maskinportenToken = hentMaskinportenToken();

        var exchangeRequest = HttpRequest.newBuilder()
            .header("Cache-Control", "no-cache")
            .header("Authorization", "Bearer " + maskinportenToken)
            .timeout(Duration.ofSeconds(3))
            .uri(URI.create(ENV.getRequiredProperty("altinn.tre.token.exchange.path")))
            .GET()
            .build();

        return hentTokenRetryable(exchangeRequest, 3);
    }

    private static String hentTokenRetryable(HttpRequest request, int retries) {
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

    private static String hentToken(HttpRequest request) {
        try (var client = byggHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
            if (response == null || response.body() == null || !responskode2xx(response)) {
                throw new TekniskException("F-157385", "Kunne ikke hente token");
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
            .connectTimeout(Duration.ofSeconds(2))
            .proxy(HttpClient.Builder.NO_PROXY)
            .build();
    }

    private String hentMaskinportenToken() {
        String endpoint = ENV.getRequiredProperty("NAIS_TOKEN_ENDPOINT");
        MaskinportenTokenRequest tokenRequest = new MaskinportenTokenRequest("maskinporten", "altinn:authorization/authorize");
        RestRequest postRequest = RestRequest.newPOSTJson(tokenRequest, URI.create(endpoint), RestConfig.forClient(AuthKlient.class));
        return restClient.send(postRequest, MaskinportenTokenResponse.class).access_token();
    }

    protected record TokenIntrospectionResponse(boolean active, String error, Consumer consumer,
                                                AuthorizationDetails authorization_details, String scope, String acr_values) {
        private record AuthorizationDetails(String type, List<String> systemuser_id, SystemuserOrg systemuser_org) {}
        // Arbeidsgivers orgnummer
        // disse 2 kommer på følgende format i json: "0192:orgno"
        private record SystemuserOrg(String ID) {}
        //Lps orgnummer
        private record Consumer(String ID) {}
    }
    protected record TokenValiderRequest(String identity_provider, String token) {}
    protected record MaskinportenTokenRequest(String identity_provider, String target) {}
    protected record MaskinportenTokenResponse(String access_token) {}

    /*
     * VI har validert tokenet
     * Vi må validere scopes fra tokenet
     * Vi må hente ut systembrukeren (systemuser_id fra Authorization_detalis) og orgnummer fra SystemuserOrg (organisasjonsnummer) for å
     * bruke dette videre i kall mot altinn autorasjon apiet med altinn.tre.base.url for å autorisere at systemet har tilgang til å sende
     * inntektsemelding på vegne av orgnummeret.
     * */
}
