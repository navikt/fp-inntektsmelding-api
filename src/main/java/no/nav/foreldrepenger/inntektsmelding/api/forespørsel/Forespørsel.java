package no.nav.foreldrepenger.inntektsmelding.api.forespørsel;

import java.time.LocalDate;
import java.util.UUID;

public record Forespørsel(UUID forespørselUuid, String orgnummer, String aktørId, LocalDate førsteUttaksdato,
                          LocalDate skjæringstidspunkt, ForespørselStatus status, YtelseType ytelseType) {
    public enum ForespørselStatus {
        UTGÅTT,
        UNDER_BEHANDLING,
        FERDIG,
    }

    public enum YtelseType {
        FORELDREPENGER,
        SVANGERSKAPSPENGER,
    }
}
