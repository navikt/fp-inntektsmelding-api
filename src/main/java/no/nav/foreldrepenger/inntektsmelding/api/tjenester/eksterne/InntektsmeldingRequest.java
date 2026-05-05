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

public record InntektsmeldingRequest(@NotNull @Valid UUID foresporselId,
                                     @Pattern(
                                         regexp = "^\\d{11}$",
                                         message = "Fødselsnummer må bestå av 11 siffer"
                                     ) @NotNull String foedselsnummer,
                                     @NotNull LocalDate startdato,
                                     @NotNull YtelseType ytelse,
                                     @NotNull @Valid InntektInfo inntekt,
                                     @Valid Refusjon refusjon,
                                     @NotNull List<@Valid Naturalytelse> naturalytelser,
                                     @NotNull String kontaktinformasjon,
                                     @NotNull String arbeidsgiverTlf,
                                     @NotNull @Valid Avsender avsender) {


    public record Refusjon(@NotNull BigDecimal beloepPerMaaned,
                           @NotNull @Valid List<RefusjonEndring> endringer) {
        public record RefusjonEndring(@NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloep, @NotNull LocalDate stardato) {}
    }


    public record Naturalytelse(@NotNull Naturalytelsetype naturalytelse,
                                @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal verdiBelop,
                                @NotNull LocalDate bortfallerFra,
                                LocalDate bortfallerTil) {
        public enum Naturalytelsetype {
            ELEKTRISK_KOMMUNIKASJON,
            AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS,
            LOSJI,
            KOST_DOEGN,
            BESØKSREISER_HJEMMET_ANNET,
            KOSTBESPARELSE_I_HJEMMET,
            RENTEFORDEL_LÅN,
            BIL,
            KOST_DAGER,
            BOLIG,
            SKATTEPLIKTIG_DEL_FORSIKRINGER,
            FRI_TRANSPORT,
            OPSJONER,
            TILSKUDD_BARNEHAGEPLASS,
            ANNET,
            BEDRIFTSBARNEHAGEPLASS,
            YRKEBIL_TJENESTLIGBEHOV_KILOMETER,
            YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS,
            INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING
        }
    }


    public record Avsender(@NotNull @Size(max = 200) String systemNavn, @NotNull @Size(max = 100) String systemVersjon) {
    }

    public record InntektInfo(@Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned, @NotNull List<Endringsårsak> endringAarsaker) {
        public record Endringsårsak(@Valid EndringsårsakType aarsak,
                                    LocalDate fom,
                                    LocalDate tom,
                                    LocalDate gjelderFra) {
            public enum EndringsårsakType {
                PERMITTERING,
                NY_STILLING,
                NY_STILLINGSPROSENT,
                SYKEFRAVÆR,
                BONUS,
                FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER,
                NYANSATT,
                MANGELFULL_RAPPORTERING_AORDNING,
                INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING,
                TARIFFENDRING,
                FERIE,
                VARIG_LØNNSENDRING,
                PERMISJON
            }
        }
    }

}
