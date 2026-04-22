package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseType;

public record InntektsmeldingFilter(@NotNull @Pattern(regexp = "^\\d{9}$") String orgnr,
                                    @Pattern(regexp = "^\\d{11}$") String fnr,
                                    @Valid UUID forespoerselId,
                                    @Valid UUID innsendingId,
                                    @Valid YtelseType ytelseType,
                                    LocalDate fom,
                                    LocalDate tom) {}
