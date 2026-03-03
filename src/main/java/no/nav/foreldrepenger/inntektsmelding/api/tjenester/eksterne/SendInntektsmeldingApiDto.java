package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.inntektsmelding.api.typer.AktørIdDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.ArbeidsgiverDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SendInntektsmeldingApiDto(@Valid UUID foresporselUuid,
                                        //todo Anja vurdere om disse er nødvendige, tenker det er lurt å benytte for validering av at det er korrekt forespørsel de sender inn data for
                                        @NotNull@Valid AktørIdDto aktørIdDto,
                                        @NotNull LocalDate førsteUttaksdato, //skal vi kalle denne førsteFraværsdato?
                                        @NotNull LocalDate skjæringstidspunkt, // skal vi kalle denne inntektsdato?
                                        @NotNull @Valid YtelseTypeDto ytelse,
                                        @NotNull @Valid ArbeidsgiverDto arbeidsgiverIdent,
                                        @NotNull @Valid SendInntektsmeldingApiDto.KontaktpersonApiDto kontaktperson,
                                        @NotNull LocalDate startdato,
                                        @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                        @NotNull List<@Valid Refusjon> refusjon,
                                        @NotNull List<@Valid BortfaltNaturalytelseApiDto> bortfaltNaturalytelsePerioder,
                                        @NotNull List<@Valid EndringsårsakerApiDto> endringAvInntektÅrsaker) {

    public record Refusjon(@NotNull LocalDate fom,
                           @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }


    public record BortfaltNaturalytelseApiDto(@NotNull LocalDate fom,
                                                  LocalDate tom,
                                                  @NotNull NaturalytelsetypeDto naturalytelsetype,
                                                  @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record EndringsårsakerApiDto(@NotNull @Valid EndringsårsakDto årsak,
                                        LocalDate fom,
                                        LocalDate tom,
                                        LocalDate bleKjentFom) {
    }

    public record KontaktpersonApiDto(@Size(max = 100) @NotNull String navn, @NotNull @Size(max = 100) String telefonnummer) {
    }
}
