package no.nav.foreldrepenger.inntektsmelding.api.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.KildesystemDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

class InntektsmeldingMapperTest {

    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final String FNR = "12345678901";
    private static final String ORGNR = "123456789";
    private static final LocalDate STARTDATO = LocalDate.of(2024, 1, 1);
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2024, 1, 1);
    private static final LocalDateTime INNSENDT_TIDSPUNKT = LocalDateTime.of(2024, 1, 2, 10, 0);
    private static final BigDecimal MÅNEDS_INNTEKT = new BigDecimal("50000.00");
    private static final BigDecimal MÅNEDS_REFUSJON = new BigDecimal("30000.00");

    @Test
    void skal_mappe_grunnleggende_felter() {
        var inntektsmelding = lagInntektsmeldingMedTommeLister();

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.inntektsmeldingId()).isEqualTo(TEST_UUID);
        assertThat(dto.fnr()).isEqualTo(FNR);
        assertThat(dto.ytelse()).isEqualTo(YtelseTypeDto.FORELDREPENGER);
        assertThat(dto.arbeidsgiver()).isEqualTo(ORGNR);
        assertThat(dto.startdato()).isEqualTo(STARTDATO);
        assertThat(dto.innsendtTidspunkt()).isEqualTo(INNSENDT_TIDSPUNKT);
        assertThat(dto.kildesystem()).isEqualTo(KildesystemDto.ARBEIDSGIVERPORTAL);
        assertThat(dto.kontaktperson().navn()).isEqualTo("Ola Nordmann");
        assertThat(dto.kontaktperson().telefonnummer()).isEqualTo("12345678");
        assertThat(dto.avsenderSystem().systemnavn()).isEqualTo("TestSystem");
        assertThat(dto.avsenderSystem().versjon()).isEqualTo("1.0");
        assertThat(dto.inntekt().beloep()).isEqualByComparingTo(MÅNEDS_INNTEKT);
        assertThat(dto.inntekt().inntektsdato()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(dto.inntekt().endringAarsaker()).isEmpty();

    }

    @Test
    void skal_mappe_inntekt_endringsårsaker() {
        var endringsårsak = new Inntektsmelding.Endringsårsaker(
            EndringsårsakDto.BONUS,
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 31),
            LocalDate.of(2024, 1, 15)
        );
        var inntektsmelding = lagInntektsmeldingBuilder(List.of(endringsårsak), List.of(), List.of(), null);

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.inntekt().endringAarsaker()).hasSize(1);
        var mappetÅrsak = dto.inntekt().endringAarsaker().getFirst();
        assertThat(mappetÅrsak.aarsak()).isEqualTo(EndringsårsakDto.BONUS);
        assertThat(mappetÅrsak.fom()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(mappetÅrsak.tom()).isEqualTo(LocalDate.of(2024, 1, 31));
        assertThat(mappetÅrsak.bleKjentFom()).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    void skal_mappe_refusjon_uten_opphørsdato() {
        var refusjonsendring = new Inntektsmelding.Refusjonsendringer(LocalDate.of(2024, 3, 1), new BigDecimal("20000.00"));
        var inntektsmelding = lagInntektsmeldingBuilder(List.of(), List.of(refusjonsendring), List.of(), null);

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.refusjon().beloepPrMnd()).isEqualByComparingTo(MÅNEDS_REFUSJON);
        assertThat(dto.refusjon().endringer()).hasSize(1);
        assertThat(dto.refusjon().endringer().getFirst().beloepPrMnd()).isEqualByComparingTo(new BigDecimal("20000.00"));
        assertThat(dto.refusjon().endringer().getFirst().fom()).isEqualTo(LocalDate.of(2024, 3, 1));
    }

    @Test
    void skal_mappe_refusjon_med_opphørsdato() {
        var opphørsdato = LocalDate.of(2024, 6, 1);
        var inntektsmelding = lagInntektsmeldingBuilder(List.of(), List.of(), List.of(), opphørsdato);

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        // Opphørsdato skal legges til som en ekstra RefusjonEndring med beløp 0
        assertThat(dto.refusjon().endringer()).hasSize(1);
        var opphørEndring = dto.refusjon().endringer().getFirst();
        assertThat(opphørEndring.beloepPrMnd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(opphørEndring.fom()).isEqualTo(opphørsdato);
    }

    @Test
    void skal_mappe_refusjon_med_endringer_og_opphørsdato() {
        var opphørsdato = LocalDate.of(2024, 6, 1);
        var refusjonsendring = new Inntektsmelding.Refusjonsendringer(LocalDate.of(2024, 3, 1), new BigDecimal("20000.00"));
        var inntektsmelding = lagInntektsmeldingBuilder(List.of(), List.of(refusjonsendring), List.of(), opphørsdato);

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        // Skal ha den originale endringen pluss opphøret
        assertThat(dto.refusjon().endringer()).hasSize(2);
        assertThat(dto.refusjon().endringer().get(0).beloepPrMnd()).isEqualByComparingTo(new BigDecimal("20000.00"));
        assertThat(dto.refusjon().endringer().get(1).beloepPrMnd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.refusjon().endringer().get(1).fom()).isEqualTo(opphørsdato);
    }

    @Test
    void skal_mappe_naturalytelser() {
        var naturalytelse = new Inntektsmelding.BortfaltNaturalytelse(
            LocalDate.of(2024, 2, 1),
            LocalDate.of(2024, 2, 28),
            NaturalytelsetypeDto.BIL,
            new BigDecimal("3000.00")
        );
        var inntektsmelding = lagInntektsmeldingBuilder(List.of(), List.of(), List.of(naturalytelse), null);

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.bortfaltNaturalytelsePerioder()).hasSize(1);
        var mappetNaturalytelse = dto.bortfaltNaturalytelsePerioder().getFirst();
        assertThat(mappetNaturalytelse.verdi()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(mappetNaturalytelse.sluttdato()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(mappetNaturalytelse.naturalytelse()).isEqualTo(NaturalytelsetypeDto.BIL);
    }

    @Test
    void skal_mappe_flere_naturalytelser() {
        var naturalytelse1 = new Inntektsmelding.BortfaltNaturalytelse(
            LocalDate.of(2024, 2, 1),
            LocalDate.of(2024, 2, 28),
            NaturalytelsetypeDto.BIL,
            new BigDecimal("3000.00")
        );
        var naturalytelse2 = new Inntektsmelding.BortfaltNaturalytelse(
            LocalDate.of(2024, 3, 1),
            null,
            NaturalytelsetypeDto.ELEKTRISK_KOMMUNIKASJON,
            new BigDecimal("500.00")
        );
        var inntektsmelding = lagInntektsmeldingBuilder(List.of(), List.of(), List.of(naturalytelse1, naturalytelse2), null);

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.bortfaltNaturalytelsePerioder()).hasSize(2);
        assertThat(dto.bortfaltNaturalytelsePerioder().get(0).naturalytelse()).isEqualTo(NaturalytelsetypeDto.BIL);
        assertThat(dto.bortfaltNaturalytelsePerioder().get(1).naturalytelse()).isEqualTo(NaturalytelsetypeDto.ELEKTRISK_KOMMUNIKASJON);
    }

    @Test
    void skal_returnere_tomme_lister_når_ingen_data() {
        var inntektsmelding = lagInntektsmeldingMedTommeLister();

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.inntekt().endringAarsaker()).isEmpty();
        assertThat(dto.refusjon().endringer()).isEmpty();
        assertThat(dto.bortfaltNaturalytelsePerioder()).isEmpty();
    }

    private Inntektsmelding lagInntektsmeldingMedTommeLister() {
        return lagInntektsmeldingBuilder(List.of(), List.of(), List.of(), null);
    }

    private Inntektsmelding lagInntektsmeldingBuilder(
        List<Inntektsmelding.Endringsårsaker> endringsårsaker,
        List<Inntektsmelding.Refusjonsendringer> refusjonsendringer,
        List<Inntektsmelding.BortfaltNaturalytelse> naturalytelser,
        LocalDate opphørsdatoRefusjon
    ) {
        return new Inntektsmelding(
            TEST_UUID,
            FNR,
            YtelseTypeDto.FORELDREPENGER,
            new OrganisasjonsnummerDto(ORGNR),
            new Inntektsmelding.Kontaktperson("Ola Nordmann", "12345678"),
            STARTDATO,
            MÅNEDS_INNTEKT,
            SKJÆRINGSTIDSPUNKT,
            INNSENDT_TIDSPUNKT,
            KildesystemDto.ARBEIDSGIVERPORTAL,
            new Inntektsmelding.AvsenderSystem("TestSystem", "1.0"),
            MÅNEDS_REFUSJON,
            opphørsdatoRefusjon,
            refusjonsendringer,
            naturalytelser,
            endringsårsaker
        );
    }
}

