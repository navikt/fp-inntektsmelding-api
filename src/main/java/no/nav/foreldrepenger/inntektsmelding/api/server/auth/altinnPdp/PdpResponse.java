package no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinnPdp;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PdpResponse(
    @JsonProperty("response")
    List<DecisionResult> response
) {
    public List<Decision> resultat() {
        return response.stream()
            .map(DecisionResult::decision)
            .toList();
    }

    public record DecisionResult(
        @JsonProperty("decision")
        Decision decision
    ) {}

    public enum Decision {
        Permit,
        Indeterminate,
        NotApplicable,
        Deny,
    }

    public boolean harTilgang() {
        return !response.isEmpty() &&
            resultat().stream().allMatch(d -> d == Decision.Permit);
    }
}
