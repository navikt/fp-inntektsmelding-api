package no.nav.foreldrepenger.inntektsmelding.api.typer;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record FødselsnummerDto(@Pattern(
    regexp = "^[\\p{M}\\p{N}\\p{L}\\p{Z}\\p{Cf}\\p{P}\\p{Sc}\\p{Sk}\n\r\t+=]*$"
) String value) {
    public String toString() {
        return this.getClass().getSimpleName() + " [soekerFnr=******]";
    }

    @JsonValue
    public @Pattern(
    regexp = "^[\\p{M}\\p{N}\\p{L}\\p{Z}\\p{Cf}\\p{P}\\p{Sc}\\p{Sk}\n\r\t+=]*$"
) @NotNull String value() {
        return this.value;
    }
}
