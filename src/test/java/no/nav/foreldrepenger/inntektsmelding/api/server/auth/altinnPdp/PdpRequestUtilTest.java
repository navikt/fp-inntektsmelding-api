package no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinnPdp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdpRequestUtilTest {

    @Test
    void skal_lage_request() {
        // Arrange
        String systemUserId = "navida_lps";
        String systemUserAtt = "urn:altinn:systemuser:uuid";
        String orgnummer = "999999999";
        String ressurs = "nav:foreldrepenger_dummy";

        // Act
        var request = PdpRequestUtil.lagPdpRequest(new PdpKlient.System(systemUserId, systemUserAtt), orgnummer, ressurs);

        // Assert
        assertThat(request).isNotNull();
        assertThat(request.request().accessSubject().getFirst().id()).isEqualTo("s1");
        assertThat(request.request().accessSubject().getFirst().attribute().getFirst().attributeId()).isEqualTo(systemUserAtt);
        assertThat(request.request().accessSubject().getFirst().attribute().getFirst().value()).isEqualTo(systemUserId);

        assertThat(request.request().action().getFirst().id()).isEqualTo("a1");

        assertThat(request.request().resource().getFirst().id()).isEqualTo("r");
        assertThat(request.request().resource().getFirst().attribute().stream().anyMatch(a -> a.attributeId().equals("urn:altinn:resource"))).isTrue();
        assertThat(request.request().resource().getFirst().attribute().stream().anyMatch(a -> a.value().equals(ressurs))).isTrue();
        assertThat(request.request().resource().getFirst().attribute().stream().anyMatch(a -> a.attributeId().equals("urn:altinn:organization:identifier-no"))).isTrue();
        assertThat(request.request().resource().getFirst().attribute().stream().anyMatch(a -> a.value().equals(orgnummer))).isTrue();
    }

}
