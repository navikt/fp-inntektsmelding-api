package no.nav.foreldrepenger.inntektsmelding.api.inntektsmelding;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

public class InntektsmeldingMapper {
    private InntektsmeldingMapper() {
        // Skjuler default
    }

    public static InntektsmeldingDto mapTilDto(Inntektsmelding inntektsmelding) {
        var kontakpersonDto = new InntektsmeldingDto.Kontaktperson(inntektsmelding.kontaktperson().navn(),
            inntektsmelding.kontaktperson().telefonnummer());
        var inntektEndringsårsaker = mapEndringsårsaker(inntektsmelding);
        var inntekt = new InntektsmeldingDto.Inntekt(inntektsmelding.månedInntekt(), inntektsmelding.skjæringstidspunkt(), inntektEndringsårsaker);
        var avsendersystemDto = new InntektsmeldingDto.AvsenderSystem(inntektsmelding.avsenderSystem().navn(),
            inntektsmelding.avsenderSystem().versjon());
        var alleRefusjonsendringer = mapRefusjon(inntektsmelding);
        var naturalytelser = mapNaturalytelser(inntektsmelding);
        var refusjon = new InntektsmeldingDto.Refusjon(inntektsmelding.månedRefusjon(), alleRefusjonsendringer);
        return new InntektsmeldingDto(inntektsmelding.inntektsmeldingUuid(),
            inntektsmelding.fnr(),
            inntektsmelding.ytelse(),
            inntektsmelding.orgnr().orgnr(),
            kontakpersonDto,
            inntektsmelding.startdato(),
            inntekt,
            inntektsmelding.innsendtTidspunkt(),
            inntektsmelding.kildesystem(),
            avsendersystemDto,
            refusjon,
            naturalytelser);
    }

    private static List<InntektsmeldingDto.Naturalytelse> mapNaturalytelser(Inntektsmelding inntektsmelding) {
        return inntektsmelding.bortfaltNaturalytelsePerioder()
            .stream()
            .map(n -> new InntektsmeldingDto.Naturalytelse(n.beløp(), n.fom(), n.naturalytelsetype()))
            .toList();
    }

    private static List<InntektsmeldingDto.InntektEndringsårsaker> mapEndringsårsaker(Inntektsmelding inntektsmelding) {
        return inntektsmelding.endringAvInntektÅrsaker()
            .stream()
            .map(e -> new InntektsmeldingDto.InntektEndringsårsaker(e.årsak(), e.fom(), e.tom(), e.bleKjentFom()))
            .toList();
    }

    private static List<InntektsmeldingDto.RefusjonEndring> mapRefusjon(Inntektsmelding inntektsmelding) {
        var refusjonsendringer = inntektsmelding.refusjonEndringer()
            .stream()
            .map(r -> new InntektsmeldingDto.RefusjonEndring(r.beløp(), r.fom()))
            .toList();
        return inntektsmelding.opphørsdatoRefusjon() == null
                                     ? refusjonsendringer
                                     : Stream.concat(
                                         refusjonsendringer.stream(),
                                         Stream.of(new InntektsmeldingDto.RefusjonEndring(BigDecimal.ZERO, inntektsmelding.opphørsdatoRefusjon()))
                                     ).toList();
    }

}
