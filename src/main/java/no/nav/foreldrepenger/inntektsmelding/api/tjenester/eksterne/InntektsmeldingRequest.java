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

public record InntektsmeldingRequest(@NotNull @Valid UUID foresporselUuid,
                                     @Pattern(
                                         regexp = "^\\d{11}$",
                                         message = "Fødselsnummer må bestå av 11 siffer"
                                     ) @NotNull String fødselsnummer,
                                     @NotNull LocalDate startdato,
                                     @NotNull YtelseType ytelse,
                                     @NotNull @Valid Kontaktperson kontaktperson,
                                     @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt, //kan inntekt noen gang være 0?
                                     @NotNull List<@Valid Refusjon> refusjon,
                                     @NotNull List<@Valid BortfaltNaturalytelse> bortfaltNaturalytelsePerioder,
                                     @NotNull List<@Valid Endringsårsaker> endringAvInntektÅrsaker,
                                     @NotNull @Valid AvsenderSystem avsenderSystem) {

    public enum YtelseType {
        FORELDREPENGER,
        SVANGERSKAPSPENGER
    }

    public record Refusjon(@NotNull LocalDate fom,
                           @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }


    public record BortfaltNaturalytelse(@NotNull LocalDate fom,
                                        LocalDate tom,
                                        @NotNull Naturalytelsetype naturalytelsetype,
                                        @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
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

    public record Endringsårsaker(@NotNull @Valid Endringsårsak årsak,
                           LocalDate fom,
                           LocalDate tom,
                           LocalDate bleKjentFom) {

        public enum Endringsårsak {
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

    public record Kontaktperson(@Size(max = 200) @NotNull String navn, @NotNull @Size(max = 50) String telefonnummer) {
    }

    public record AvsenderSystem(@NotNull @Size(max = 200) String navn, @NotNull @Size(max = 100) String versjon) {
    }

}
