package no.nav.foreldrepenger.inntektsmelding.api.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.KildesystemDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

public record InntektsmeldingDto(UUID inntektsmeldingId,
                                 String fnr,
                                 YtelseTypeDto ytelse,
                                 String arbeidsgiver,
                                 Kontaktperson kontaktperson,
                                 LocalDate startdato,
                                 Inntekt inntekt,
                                 LocalDateTime innsendtTidspunkt,
                                 KildesystemDto kildesystem, // TODO trengs denne?
                                 AvsenderSystem avsenderSystem,
                                 Refusjon refusjon,
                                 List<Naturalytelse> bortfaltNaturalytelsePerioder) {

    public record Inntekt(BigDecimal beloep, LocalDate inntektsdato, List<InntektEndringsårsaker> endringAarsaker) {
    }

    public record Refusjon(BigDecimal beloepPrMnd, List<RefusjonEndring> endringer) {
    }

    public record InntektEndringsårsaker(EndringsårsakDto aarsak, LocalDate fom, LocalDate tom, LocalDate bleKjentFom) {
    }

    public record RefusjonEndring(BigDecimal beloepPrMnd, LocalDate fom) {
    }

    public record AvsenderSystem(String systemnavn, String versjon) {
    }

    public record Kontaktperson(String navn, String telefonnummer) {
    }

    public record Naturalytelse(BigDecimal verdi, LocalDate sluttdato, NaturalytelsetypeDto naturalytelse) {
    }
}

