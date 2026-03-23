package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

record InntektsmeldingRequestDto(UUID forespørselUuid,
                                 String fødselsnummer,
                                 OrganisasjonsnummerDto orgnummer,
                                 LocalDate startDato,
                                 YtelseTypeDto ytelseTypeDto,
                                 KontaktpersonDto kontaktperson,
                                 BigDecimal inntekt,
                                 List<RefusjonDto> refusjonsperioder,
                                 List<BortfaltNaturalYtelseDto> bortfaltNaturalytelsePerioder,
                                 List<EndringsårsakerDto> endringsårsaker,
                                 AvsenderSystemDto avsenderSystem
) {
    @Override
    public String toString() {
        return "InntektsmeldingRequestDto{" +
            "forespørselUuid=" + forespørselUuid +
            ", fødselsnummer=" + (fødselsnummer != null && fødselsnummer.length() > 7
                                  ? "***" + fødselsnummer.substring(7)
                                  : "***") +
            ", orgnummer=" + orgnummer +
            ", startDato=" + startDato +
            ", ytelseTypeDto=" + ytelseTypeDto +
            ", kontaktperson=" + kontaktperson +
            ", inntekt=" + inntekt +
            ", refusjonsperioder=" + refusjonsperioder +
            ", bortfaltNaturalytelsePerioder=}" + bortfaltNaturalytelsePerioder +
            ", endringsårsaker=" + endringsårsaker +
            ", avsenderSystem=" + avsenderSystem +
            '}';
    }

    record KontaktpersonDto(String navn, String telefonnummer) {
    }

    record RefusjonDto(LocalDate fom, BigDecimal beløp) {
    }

    record BortfaltNaturalYtelseDto(LocalDate fom,
                                    LocalDate tom,
                                    NaturalytelsetypeDto naturalytelsetype,
                                    BigDecimal beløp) {
    }

    record EndringsårsakerDto(EndringsårsakDto årsak,
                              LocalDate fom,
                              LocalDate tom,
                              LocalDate bleKjentFom) {
    }

    record AvsenderSystemDto(String navn, String versjon) {
    }
}
