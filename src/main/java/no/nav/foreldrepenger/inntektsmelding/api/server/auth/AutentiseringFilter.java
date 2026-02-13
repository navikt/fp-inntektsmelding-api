package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.vedtak.sikkerhet.jaxrs.AuthenticationFilterDelegate;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AutentiseringFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(AutentiseringFilter.class);

    @Context
    private ResourceInfo resourceinfo;

    public AutentiseringFilter() {
        // Ingenting
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        AuthenticationFilterDelegate.fjernKontekst();
    }

    @Override
    public void filter(ContainerRequestContext req) {
        assertValidRequest(req);
    }

    void assertValidRequest(ContainerRequestContext req) {
        var method = getResourceinfo().getResourceMethod();
        Optional<TokenString> tokenFromHeader = getTokenFromHeader(req);

        if (tokenFromHeader.isEmpty()) {
            throw new WebApplicationException("Mangler token", Response.Status.UNAUTHORIZED);
        }
        LOG.trace("{} i klasse {}", method.getName(), method.getDeclaringClass());
        fjernKontekstHvisFinnes();
        var authKlient = AuthKlient.instance();
        authKlient.validerOgSettKontekst(tokenFromHeader.get());
    }

    public static Optional<TokenString> getTokenFromHeader(ContainerRequestContext request) {
        return Optional.ofNullable(request.getHeaderString(HttpHeaders.AUTHORIZATION))
            .filter(headerValue -> headerValue.startsWith(OpenIDToken.OIDC_DEFAULT_TOKEN_TYPE))
            .map(headerValue -> headerValue.substring(OpenIDToken.OIDC_DEFAULT_TOKEN_TYPE.length()))
            .map(TokenString::new);
    }

    private void fjernKontekstHvisFinnes() {
        if (KontekstHolder.harKontekst()) {
            LOG.info("Kall til {} hadde kontekst {}", getResourceinfo().getResourceMethod().getName(), KontekstHolder.getKontekst().getKompaktUid());
            KontekstHolder.fjernKontekst();
            MDC.clear();
        }
    }

    private ResourceInfo getResourceinfo() {
        return resourceinfo;
    }
}
