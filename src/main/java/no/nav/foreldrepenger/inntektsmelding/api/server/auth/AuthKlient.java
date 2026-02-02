package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

import java.net.URI;
import java.util.List;

@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED)
public class AuthKlient  {

    private final RestClient restClient;
    private static AuthKlient instance = new AuthKlient();

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
        String endpoint = Environment.current().getRequiredProperty("NAIS_TOKEN_INTROSPECTION_ENDPOINT");
        TokenValiderRequest tokeValiderRequest = new TokenValiderRequest("maskinporten", tokenString.token());
        RestRequest postRequest = RestRequest.newPOSTJson(tokeValiderRequest, URI.create(endpoint), RestConfig.forClient(AuthKlient.class));
        TokenIntrospectionResponse response = restClient.send(postRequest, TokenIntrospectionResponse.class);
        if (!response.active) {
            // FEIL
        }
        var orgnummer = response.authorization_details.systemuser_org.ID;
        if (orgnummer == null || orgnummer.isBlank()) {
            // FEIL
        }

    }

    protected record TokenIntrospectionResponse(boolean active, String error, Consumer consumer, AuthorizationDetails authorization_details) {
        private record AuthorizationDetails(String type, List<String> systemuser_id, SystemuserOrg systemuser_org) {}
        // disse kommer på følgende format i json: "0192:orgno"
        private record SystemuserOrg(String ID) {}
        private record Consumer(String ID) {}
    }
    protected record TokenValiderRequest(String identity_provider, String token) {}

    /*
    * VI har validert tokenet
    * Vi må validere scopes fra tokenet
    * Vi må hente ut systembrukeren (systemuser_id fra Authorization_detalis) og orgnummer fra SystemuserOrg (organisasjonsnummer) for å
    * bruke dette videre i kall mot altinn autorasjon apiet med altinn.tre.base.url for å autorisere at systemet har tilgang til å sende
    * inntektsemelding på vegne av orgnummeret.
    * */
}
