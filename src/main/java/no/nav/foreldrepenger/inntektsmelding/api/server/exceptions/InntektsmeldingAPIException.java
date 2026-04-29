package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import jakarta.ws.rs.core.Response;

public class InntektsmeldingAPIException extends RuntimeException {
    private final EksponertFeilmelding eksponertFeilmelding;
    private final Response.Status status;

    public InntektsmeldingAPIException(EksponertFeilmelding eksponertFeilmelding, Response.Status status) {
        super(eksponertFeilmelding.toString());
        this.eksponertFeilmelding = eksponertFeilmelding;
        this.status = status;
    }

    public InntektsmeldingAPIException(EksponertFeilmelding eksponertFeilmelding, Response.Status status, Throwable cause) {
        super(eksponertFeilmelding.toString(), cause);
        this.eksponertFeilmelding = eksponertFeilmelding;
        this.status = status;
    }

    public EksponertFeilmelding getFeilmelding() {
        return eksponertFeilmelding;
    }

    public Response.Status getStatus() {
        return status;
    }
}
