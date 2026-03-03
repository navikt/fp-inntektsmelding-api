package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.InntektsmeldingAPIException;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class AuthTjenesteTest {
    private static final String KORREKT_SCOPE = "nav:inntektsmelding/foreldrepenger";
    @Mock
    private AuthKlient authKlient;

    private AuthTjeneste authTjeneste;

    @BeforeEach
    void setUp() {
        authTjeneste = new AuthTjeneste(authKlient);
    }

    @Test
    void skal_feile_om_ikke_rett_scope() {
        // Arrange
        var tokenString = new TokenString("123");
        var authDetails = new TokenIntrospectionResponse.AuthorizationDetails("type",
            List.of("systemuser"),
            new TokenIntrospectionResponse.SystemuserOrg("mitt_orgnr"));
        when(authKlient.introspectToken(tokenString)).thenReturn(new TokenIntrospectionResponse(true, null, new TokenIntrospectionResponse.Consumer("minId"), List.of(authDetails), "ugyldigScope", null));

        // Act
        var ex = assertThrows(InntektsmeldingAPIException.class, () -> authTjeneste.validerOgSettKontekst(tokenString));

        // Assert
        assertThat(ex.getFeilmelding()).isEqualTo(EksponertFeilmelding.FEIL_SCOPE);
    }

    @Test
    void skal_feile_om_token_ikke_aktivt() {
        // Arrange
        var tokenString = new TokenString("123");
        var authDetails = new TokenIntrospectionResponse.AuthorizationDetails("type",
            List.of("systemuser"),
            new TokenIntrospectionResponse.SystemuserOrg("mitt_orgnr"));
        when(authKlient.introspectToken(tokenString)).thenReturn(new TokenIntrospectionResponse(false, null, new TokenIntrospectionResponse.Consumer("minId"), List.of(authDetails), KORREKT_SCOPE, null));

        // Act
        var ex = assertThrows(InntektsmeldingAPIException.class, () -> authTjeneste.validerOgSettKontekst(tokenString));

        // Assert
        assertThat(ex.getFeilmelding()).isEqualTo(EksponertFeilmelding.UTGÅTT_TOKEN);
    }

    @Test
    void skal_feile_om_token_ikke_inneholder_påkrevd_felt() {
        // Arrange
        var tokenString = new TokenString("123");
        when(authKlient.introspectToken(tokenString)).thenReturn(new TokenIntrospectionResponse(true, null, new TokenIntrospectionResponse.Consumer("minId"), List.of(), KORREKT_SCOPE, null));

        // Act
        var ex = assertThrows(InntektsmeldingAPIException.class, () -> authTjeneste.validerOgSettKontekst(tokenString));

        // Assert
        assertThat(ex.getFeilmelding()).isEqualTo(EksponertFeilmelding.UGYLDIG_TOKEN);
    }

    @Test
    void skal_sette_kontektst_om_token_er_gyldig() {
        // Arrange
        var tokenString = new TokenString("123");
        var systemuser = "systemuser";
        var orgnr = "999999999";
        var authDetails = new TokenIntrospectionResponse.AuthorizationDetails("type",
            List.of(systemuser),
            new TokenIntrospectionResponse.SystemuserOrg(orgnr));
        when(authKlient.introspectToken(tokenString)).thenReturn(new TokenIntrospectionResponse(true, null, new TokenIntrospectionResponse.Consumer("minId"), List.of(authDetails), KORREKT_SCOPE, null));

        // Act
        authTjeneste.validerOgSettKontekst(tokenString);

        // Assert
        var kontekst = KontekstHolder.getKontekst();
        assertThat(kontekst).isInstanceOf(TokenKontekst.class);
        var tokenKontekst = (TokenKontekst) kontekst;
        assertThat(tokenKontekst.getSystemUserId()).isEqualTo(systemuser);
        assertThat(tokenKontekst.getOrganisasjonNummer().orgnr()).isEqualTo(orgnr);
    }
}
