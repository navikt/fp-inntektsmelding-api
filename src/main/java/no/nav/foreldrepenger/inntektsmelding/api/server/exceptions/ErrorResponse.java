package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;
//Todo vi må standardisere feltnavn med sykepenger, når har vi ulikt navn på feilreferanse - de har referanseId
public record ErrorResponse(String feilkode, String feilmelding, String feilreferanse) {}

