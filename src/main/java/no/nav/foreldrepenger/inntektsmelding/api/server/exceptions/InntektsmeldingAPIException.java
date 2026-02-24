package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import jakarta.ws.rs.core.Response;

import no.nav.vedtak.log.mdc.MDCOperations;

import java.util.Optional;

public class InntektsmeldingAPIException extends RuntimeException {
    private final EksponertFeilmelding eksponertFeilmelding;
    private final Response.Status status;
    private final String callId;

    public InntektsmeldingAPIException(EksponertFeilmelding eksponertFeilmelding, Response.Status status) {
        super(eksponertFeilmelding.toString());
        this.eksponertFeilmelding = eksponertFeilmelding;
        this.status = status;
        this.callId = Optional.ofNullable(MDCOperations.getCallId()).orElseGet(MDCOperations::generateCallId);
    }

    public InntektsmeldingAPIException(EksponertFeilmelding eksponertFeilmelding, Response.Status status, Throwable cause) {
        super(eksponertFeilmelding.toString(), cause);
        this.eksponertFeilmelding = eksponertFeilmelding;
        this.status = status;
        this.callId = Optional.ofNullable(MDCOperations.getCallId()).orElseGet(MDCOperations::generateCallId);
    }

    public EksponertFeilmelding getFeilmelding() {
        return eksponertFeilmelding;
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getCallId() {
        return callId;
    }
}
