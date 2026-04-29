package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String feilkode, String feilmelding, String feilreferanseId) {

    public ErrorResponse(String feilkode, String feilmelding) {
        this(feilkode, feilmelding, null);
    }
}

