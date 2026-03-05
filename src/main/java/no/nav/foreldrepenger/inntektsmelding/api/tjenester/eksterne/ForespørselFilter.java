package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.inntektsmelding.api.typer.Status;

import java.time.LocalDate;
import java.util.UUID;

// TODO Hva gjør vi med forespørselId?
public record ForespørselFilter(@NotNull @Pattern(regexp = "^\\d{9}$") String orgnr, @Pattern(regexp = "^\\d{11}$") String fnr, @Valid UUID forespørselId, @Valid Status status, LocalDate fom, LocalDate tom) {
}
