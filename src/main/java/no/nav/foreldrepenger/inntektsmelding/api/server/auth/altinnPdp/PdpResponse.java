package no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinnPdp;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

    public class PdpResponse {
        @JsonProperty("response")
        private List<DecisionResult> response;

        public PdpResponse() {}

        public PdpResponse(List<DecisionResult> response) {
            this.response = response;
        }

        public List<DecisionResult> getResponse() {
            return response;
        }

        public void setResponse(List<DecisionResult> response) {
            this.response = response;
        }

        public List<Decision> resultat() {
            return response.stream()
                .map(DecisionResult::getDecision)
                .toList();
        }

        public boolean harTilgang() {
            return !response.isEmpty() &&
                resultat().stream().allMatch(d -> d == Decision.Permit);
        }

}
