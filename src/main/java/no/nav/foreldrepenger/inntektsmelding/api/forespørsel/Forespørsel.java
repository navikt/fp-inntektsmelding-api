package no.nav.foreldrepenger.inntektsmelding.api.forespørsel;

import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record Forespørsel(UUID forespørselUuid, String orgnummer, String fødselsnummer, LocalDate førsteUttaksdato,
                          LocalDate skjæringstidspunkt, ForespørselStatus status, YtelseTypeDto ytelseType, LocalDateTime opprettetTid) {
    public enum ForespørselStatus {
        UTGÅTT,
        UNDER_BEHANDLING,
        FERDIG,
    }
}
