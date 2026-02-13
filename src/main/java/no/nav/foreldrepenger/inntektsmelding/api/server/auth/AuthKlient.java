package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED)
public class AuthKlient {
    private static final Logger LOG = LoggerFactory.getLogger(AuthKlient.class);
    private static final Environment ENV = Environment.current();
    private static AuthKlient instance = new AuthKlient();
    private final RestClient restClient;

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

    //TODO: legge inn feilhåndtering og logging, og vurdere om denne burde ligge i autentiseringsfilteret istedenfor i en egen klient
    public void validerOgSettKontekst(TokenString tokenString) {

        // Autentisering - valider token
        String endpoint = ENV.getRequiredProperty("NAIS_TOKEN_INTROSPECTION_ENDPOINT");
        TokenValiderRequest tokenValiderRequest = new TokenValiderRequest("maskinporten", tokenString.token());
        RestRequest postRequest = RestRequest.newPOSTJson(tokenValiderRequest, URI.create(endpoint), RestConfig.forClient(AuthKlient.class));
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

        var tokenKontekst = new TokenKontekst(
            response.consumer.ID,
            response.consumer.ID,
            response.authorization_details.systemuser_org.ID,
            response.authorization_details.systemuser_id.getFirst());

        KontekstHolder.setKontekst(tokenKontekst);
    }

    protected record TokenIntrospectionResponse(boolean active, String error, Consumer consumer,
                                                AuthorizationDetails authorization_details, String scope, String acr_values) {
        private record AuthorizationDetails(String type, List<String> systemuser_id, SystemuserOrg systemuser_org) {
        }

        // Arbeidsgivers orgnummer
        // kommer på følgende format i json: "0192:orgno"
        private record SystemuserOrg(String ID) {
        }

        //Lps orgnummer
        // kommer på følgende format i json: "0192:orgno"
        private record Consumer(String ID) {
        }
    }

    protected record TokenValiderRequest(String identity_provider, String token) {
    }
}
