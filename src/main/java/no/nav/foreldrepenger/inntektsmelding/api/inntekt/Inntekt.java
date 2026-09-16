package no.nav.foreldrepenger.inntektsmelding.api.inntekt;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

public record Inntekt(Map<YearMonth, BigDecimal> inntektPerMåned, BigDecimal gjennomsnitt) {

}
