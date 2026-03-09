package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.inntektsmelding.api.typer.StatusDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

// I fp-inntektsmelding søker man på opprettet_dato med fom, og tom herfra, burde dette vært komunisert bedre i navn på felt?
public record ForespørselFilter(@NotNull @Pattern(regexp = "^\\d{9}$") String orgnr,
                                @Pattern(regexp = "^\\d{11}$") String fnr,
                                @Valid UUID forespørselId,
                                @Valid StatusDto status,
                                @Valid YtelseTypeDto ytelseType,
                                LocalDate fom,
                                LocalDate tom) {
}
