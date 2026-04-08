package no.nav.foreldrepenger.inntektsmelding.api.forespørsel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseType;

public record Forespørsel(UUID forespørselUuid, Organisasjonsnummer orgnummer, String fødselsnummer, LocalDate førsteUttaksdato,
                          LocalDate skjæringstidspunkt, ForespørselStatus status, YtelseType ytelseType, LocalDateTime opprettetTid) {

}
