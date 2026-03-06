package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.InntektsmeldingAPIException;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;
import no.nav.vedtak.sikkerhet.oidc.token.texas.IdProvider;
import no.nav.vedtak.sikkerhet.oidc.token.texas.IntrospectTokenRequest;
import no.nav.vedtak.sikkerhet.oidc.token.texas.TexasTokenKlient;

public class AuthTjeneste {
    private static final Environment ENV = Environment.current();
    private static final Logger LOG = LoggerFactory.getLogger(AuthTjeneste.class);
    private final TexasTokenKlient tokenKlient;

    public AuthTjeneste() {
        this(TexasTokenKlient.instance());
    }

    protected AuthTjeneste(TexasTokenKlient tokenKlient) {
        this.tokenKlient = tokenKlient;
    }

    public void validerOgSettKontekst(TokenString tokenString) {

        var response = tokenKlient.introspectToken(new IntrospectTokenRequest(IdProvider.MASKINPORTEN, tokenString.token()));

        if (!response.active()) {
            throw new InntektsmeldingAPIException(EksponertFeilmelding.UTGÅTT_TOKEN, Response.Status.UNAUTHORIZED);
        }

        List<String> scopes = List.of(response.scope().split(" "));
        var gyldigScope = "nav:inntektsmelding/foreldrepenger";
        boolean harGyldigScope = scopes.stream().anyMatch(s -> s.equals(gyldigScope));

        if (!harGyldigScope) {
            throw new InntektsmeldingAPIException(EksponertFeilmelding.FEIL_SCOPE, Response.Status.UNAUTHORIZED);
        }

        if (response.authorization_details() == null || response.authorization_details().isEmpty()) {
            throw new InntektsmeldingAPIException(EksponertFeilmelding.UGYLDIG_TOKEN, Response.Status.UNAUTHORIZED);
        }
        var tokenKontekst = new TokenKontekst(
            response.consumer().id(),
            response.consumer().id(),
            response.authorization_details().getFirst().systemuser_org().id(),
            response.authorization_details().getFirst().systemuser_id().getFirst());

        if (!ENV.isProd()) {
            LOG.info("Token validering vellykket, consumerId: {}, systemuser_org: {}, systemuser_id: {}",
                response.consumer().id(), response.authorization_details().getFirst().systemuser_org().id(), response.authorization_details().getFirst().systemuser_id().getFirst());
        }

        KontekstHolder.setKontekst(tokenKontekst);
    }
}
