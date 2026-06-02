package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseType;

public record InntektsmeldingRequest(@NotNull @Valid UUID forespoerselId,
                                     @Pattern(
                                         regexp = "^\\d{11}$",
                                         message = "Fødselsnummer må bestå av 11 siffer"
                                     ) @NotNull String soekerFnr,
                                     @NotNull LocalDate startdato,
                                     @NotNull YtelseType ytelse,
                                     @NotNull @Valid InntektInfo inntekt,
                                     @Valid Refusjon refusjon,
                                     @NotNull List<@Valid Naturalytelse> naturalytelser,
                                     @NotNull Kontaktinformasjon kontaktinformasjon,
                                     @NotNull @Valid Avsender avsender) {


    public record InntektInfo(@NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned, @NotNull List<Endringsaarsak> endringAarsaker) {
        public record Endringsaarsak(@Valid InntektsmeldingRequest.InntektInfo.Endringsaarsak.EndringsaarsakType aarsak,
                                     LocalDate fom,
                                     LocalDate tom,
                                     LocalDate gjelderFra) {
            public enum EndringsaarsakType {
                Permittering,
                NyStilling,
                NyStillingsprosent,
                Sykefravaer,
                Bonus,
                Ferietrekk,
                Nyansatt,
                MangelfullRapporteringAordning,
                InntektIkkeRapportertEndaAordning,
                Tariffendring,
                Ferie,
                VarigLoennsendring,
                Permisjon
            }
        }
    }
    public record Refusjon(@NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned,
                           @NotNull @Valid List<RefusjonEndring> endringer) {
        public record RefusjonEndring(@NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned, @NotNull LocalDate stardato) {}

    }

    public record Kontaktinformasjon(@NotNull String arbeidsgiverNavn,  @NotNull String arbeidsgiverTlf) {}

    public record Naturalytelse(@NotNull Naturalytelsetype naturalytelse,
                                @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned,
                                @NotNull LocalDate bortfallerFra,
                                LocalDate bortfallerTil) {
        public enum Naturalytelsetype {
            ElektroniskKommunikasjon,
            AksjerGrunnfondsbevisTilUnderkurs,
            Losji,
            KostDoegn,
            BesoeksreiserHjemmetAnnet,
            KostbesparelseIHjemmet,
            RentefordelLaan,
            Bil,
            KostDager,
            Bolig,
            SkattepliktigDelForsikringer,
            FriTransport,
            Opsjoner,
            TilskuddBarnehageplass,
            Annet,
            Bedriftsbarnehageplass,
            YrkebilTjenestligbehovKilometer,
            YrkebilTjenestligbehovListepris,
            InnbetalingTilUtenlandskPensjonsordning
            }

    }

    public record Avsender(@NotNull @Size(max = 200) String systemNavn, @NotNull @Size(max = 100) String systemVersjon) {

    }
}
