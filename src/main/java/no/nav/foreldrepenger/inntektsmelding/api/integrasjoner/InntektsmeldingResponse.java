package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.KildesystemDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

record InntektsmeldingResponse(
    @NotNull UUID inntektsmeldingUuid,
    @Pattern(regexp = "^\\d{11}$") @NotNull String fnr,
    @NotNull @Valid YtelseTypeDto ytelse,
    @NotNull @Valid OrganisasjonsnummerDto orgnr,
    @NotNull @Valid Kontaktperson kontaktperson,
    @NotNull LocalDate startdato,
    @NotNull LocalDate skjæringstidspunkt,
    @NotNull BigDecimal månedInntekt,
    @NotNull LocalDateTime innsendtTidspunkt,
    @NotNull @Valid KildesystemDto kildesystem,
    @NotNull @Valid AvsenderSystem avsenderSystem,
    BigDecimal månedRefusjon,
    @NotNull LocalDate opphørsdatoRefusjon,
    @NotNull List<@Valid RefusjonEndring> refusjonsendringer,
    @NotNull List<@Valid BortfaltNaturalytelse> bortfaltNaturalytelsePerioder,
    @NotNull List<@Valid Endringsårsaker> endringAvInntektÅrsaker) {

    record RefusjonEndring(LocalDate fom,
                               BigDecimal beløp) {
    }

    record BortfaltNaturalytelse(LocalDate fom,
                                        LocalDate tom,
                                        NaturalytelsetypeDto naturalytelsetype,
                                        BigDecimal beløp) {
    }

    record Endringsårsaker(EndringsårsakDto årsak,
                                  LocalDate fom,
                                  LocalDate tom,
                                  LocalDate bleKjentFom) {
    }

    record Kontaktperson(
        String telefonnummer,
        String navn
    ) {
    }

    record AvsenderSystem(
        String navn,
        String versjon
    ) {
    }
}
