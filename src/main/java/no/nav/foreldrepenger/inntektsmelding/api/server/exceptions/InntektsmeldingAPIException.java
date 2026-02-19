package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import jakarta.ws.rs.core.Response;

public class InntektsmeldingAPIException extends RuntimeException {
    private final Feilmelding feilmelding;
    private final Response.Status status;

    public InntektsmeldingAPIException(Feilmelding feilmelding, Response.Status status) {
        super(feilmelding.toString());
        this.feilmelding = feilmelding;
        this.status = status;
    }

    public InntektsmeldingAPIException(Feilmelding feilmelding, Response.Status status, Throwable cause) {
        super(feilmelding.toString(), cause);
        this.feilmelding = feilmelding;
        this.status = status;
    }

    public Feilmelding getFeilmelding() {
        return feilmelding;
    }

    public Response.Status getStatus() {
        return status;
    }
}
