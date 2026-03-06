package no.nav.foreldrepenger.inntektsmelding.api.forespørsel;

import no.nav.foreldrepenger.inntektsmelding.api.typer.StatusDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ForespørselDto (UUID forespørselUuid, String orgnummer, String fødselsnummer, LocalDate førsteUttaksdato,
                              LocalDate skjæringstidspunkt, StatusDto status, YtelseTypeDto ytelseType, LocalDateTime opprettetTid) {

}
