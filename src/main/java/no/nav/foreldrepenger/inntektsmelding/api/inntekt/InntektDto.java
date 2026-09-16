package no.nav.foreldrepenger.inntektsmelding.api.inntekt;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record InntektDto(@NotNull Map<YearMonth, BigDecimal> inntektPerMaaned, @NotNull BigDecimal gjennomsnitt) {

}
