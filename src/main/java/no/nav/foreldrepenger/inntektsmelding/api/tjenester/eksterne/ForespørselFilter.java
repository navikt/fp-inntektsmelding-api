package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.inntektsmelding.api.typer.Status;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

import java.time.LocalDate;
import java.util.UUID;

// I fp-inntektsmelding søker man på opprettet_dato med fom, og tom herfra, burde dette vært komunisert bedre i navn på felt?
public record ForespørselFilter(@NotNull @Pattern(regexp = "^\\d{9}$") String orgnr,
                                @Pattern(regexp = "^\\d{11}$") String fnr,
                                @Valid UUID forespørselId,
                                @Valid Status status,
                                @Valid YtelseTypeDto ytelseType,
                                LocalDate fom,
                                LocalDate tom) {
}
