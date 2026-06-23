package no.nav.foreldrepenger.inntektsmelding.api.forespørsel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.inntektsmelding.api.typer.StatusDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

public record ForespørselDto(@NotNull Long loepenr, @NotNull UUID forespoerselId, @NotNull @Pattern(regexp = "^\\d{9}$") String orgnr, @NotNull @Pattern(regexp = "^\\d{11}$") String soekerFnr, @NotNull LocalDate startdato,
                             @NotNull LocalDate inntektsdato, @NotNull StatusDto status, @NotNull YtelseTypeDto ytelseType, @NotNull LocalDateTime opprettetTid) {

}
