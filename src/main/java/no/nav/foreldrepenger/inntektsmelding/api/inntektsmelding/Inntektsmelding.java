package no.nav.foreldrepenger.inntektsmelding.api.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.InntektsmeldingStatus;
import no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;
//Intern inntektsmelding record
public record Inntektsmelding(
    Long loepenr,
    UUID inntektsmeldingUuid,
    String fnr,
    YtelseTypeDto ytelse,
    Organisasjonsnummer orgnr,
    Kontaktperson kontaktperson,
    LocalDate startdato,
    BigDecimal månedInntekt,
    LocalDate skjæringstidspunkt,
    LocalDateTime innsendtTidspunkt,
    AvsenderSystem avsenderSystem,
    BigDecimal månedRefusjon,
    LocalDate opphørsdatoRefusjon,
    List<Refusjon> refusjon,
    List<BortfaltNaturalytelse> bortfaltNaturalytelsePerioder,
    List<Endringsårsaker> endringAvInntektÅrsaker,
    InntektsmeldingStatus status) {

    public record Refusjon(LocalDate fom,
                           BigDecimal beløp) {
    }

    public record BortfaltNaturalytelse(LocalDate fom,
                                        LocalDate tom,
                                        NaturalytelsetypeDto naturalytelsetype,
                                        BigDecimal beløp) {
    }

    public record Endringsårsaker(EndringsårsakDto årsak,
                                  LocalDate fom,
                                  LocalDate tom,
                                  LocalDate bleKjentFom) {
    }

    public record Kontaktperson(
        String navn,
        String telefonnummer
        ) {
    }

    public record AvsenderSystem(
        String navn,
        String versjon
    ) {
    }
}
