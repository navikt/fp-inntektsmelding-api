package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

import java.net.URI;

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
        TokenValiderResponse response = restClient.send(postRequest, TokenValiderResponse.class);
        if (!response.active) {
            // FEIL
        }

    }

    protected record TokenValiderResponse(boolean active, String error, AuthorizationDetails authorization_details) {
        private record AuthorizationDetails(String type, SystemuserOrg systemuser_org) {}
        private record SystemuserOrg(String ID) {}
    }
    protected record TokenValiderRequest(String identity_provider, String token) {}
}
