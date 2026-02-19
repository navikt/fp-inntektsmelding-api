package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import jakarta.ws.rs.core.Response;

public class InntektsmeldingAPIException extends RuntimeException {
    private final String feilmelding;
    private final Response.Status status;

    public InntektsmeldingAPIException(String feilmelding, Response.Status status) {
        super(feilmelding);
        this.feilmelding = feilmelding;
        this.status = status;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public Response.Status getStatus() {
        return status;
    }
}
