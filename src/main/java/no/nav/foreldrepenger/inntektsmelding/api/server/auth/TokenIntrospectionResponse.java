package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import java.util.List;

public record TokenIntrospectionResponse(boolean active, String error, Consumer consumer,
                                         List<AuthorizationDetails> authorization_details,
                                         String scope,
                                         String acr_values) {

    public record AuthorizationDetails(String type, List<String> systemuser_id, SystemuserOrg systemuser_org) {
    }

    // Arbeidsgivers orgnummer
    // kommer på følgende format i json: "0192:orgno"
    public record SystemuserOrg(String ID) {
    }

    //Lps orgnummer
    // kommer på følgende format i json: "0192:orgno"
    public record Consumer(String ID) {
    }
}
