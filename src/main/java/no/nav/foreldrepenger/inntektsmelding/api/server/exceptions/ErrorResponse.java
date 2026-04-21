package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

public record ErrorResponse(String feilkode, String feilmelding, String feilreferanse) {}

