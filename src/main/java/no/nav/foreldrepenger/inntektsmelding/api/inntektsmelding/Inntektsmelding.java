package no.nav.foreldrepenger.inntektsmelding.api.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.KildesystemDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

public record Inntektsmelding(
    UUID inntektsmeldingUuid,
    String fnr,
    YtelseTypeDto ytelse,
    Organisasjonsnummer orgnr,
    Kontaktperson kontaktperson,
    LocalDate startdato,
    BigDecimal månedInntekt,
    LocalDate skjæringstidspunkt,
    LocalDateTime innsendtTidspunkt,
    KildesystemDto kildesystem,
    AvsenderSystem avsenderSystem,
    BigDecimal månedRefusjon,
    LocalDate opphørsdatoRefusjon,
    List<Refusjonsendringer> refusjonEndringer,
    List<BortfaltNaturalytelse> bortfaltNaturalytelsePerioder,
    List<Endringsårsaker> endringAvInntektÅrsaker) {

    public record Refusjonsendringer(LocalDate fom,
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
