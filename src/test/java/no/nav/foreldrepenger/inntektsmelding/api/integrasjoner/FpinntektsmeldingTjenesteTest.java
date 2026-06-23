package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.api.typer.InntektsmeldingStatus;

import no.nav.foreldrepenger.inntektsmelding.felles.InntektsmeldingStatusDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne.InntektsmeldingRequest;
import no.nav.foreldrepenger.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseType;
import no.nav.foreldrepenger.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.foreldrepenger.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.foreldrepenger.inntektsmelding.felles.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.KontaktpersonDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel.ForespørselFilterRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel.ForespørselResponse;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;

@ExtendWith(MockitoExtension.class)
class FpinntektsmeldingTjenesteTest {
    @Mock
    private FpinntektsmeldingKlient fpinntektsmeldingKlient;

    private FpinntektsmeldingTjeneste fpinntektsmeldingTjeneste;

    @BeforeEach
    void setUp() {
        fpinntektsmeldingTjeneste = new FpinntektsmeldingTjeneste(fpinntektsmeldingKlient);
    }

    @Test
    void skal_hente_bestemt_forespørsel() {
        var orgnummer = "999999999";
        var uuid = UUID.randomUUID();
        var fødselsnummer = "123";
        var response = new ForespørselResponse(null, uuid, new OrganisasjonsnummerDto(orgnummer), new FødselsnummerDto(fødselsnummer),
            LocalDate.now(), LocalDate.now(), ForespørselStatusDto.UNDER_BEHANDLING, YtelseTypeDto.FORELDREPENGER, LocalDateTime.now());
        when(fpinntektsmeldingKlient.hentForespørsel(uuid)).thenReturn(response);
        var forespørsel = fpinntektsmeldingTjeneste.hentForespørsel(uuid);
        assertThat(forespørsel.orgnummer().orgnr()).isEqualTo(orgnummer);
        assertThat(forespørsel.ytelseType()).isEqualTo(YtelseType.FORELDREPENGER);
        assertThat(forespørsel.fødselsnummer()).isEqualTo(fødselsnummer);
    }

    @Test
    void skal_hente_tom_liste_forespørsler() {
        var orgnummer = "999999999";
        when(fpinntektsmeldingKlient.hentForespørsler(new ForespørselFilterRequest(new OrganisasjonsnummerDto(orgnummer),
            null,
            null,
            null,
            null,
            null,
            null))).thenReturn(
            List.of());
        var forespørsler = fpinntektsmeldingTjeneste.hentForespørsler(orgnummer, null, null, null, null, null, null);
        assertThat(forespørsler).isEmpty();
    }

    @Test
    void skal_hente_liste_forespørsler() {
        var orgnummer = "999999999";
        var fødselsnummer = "123";
        var response1 = new ForespørselResponse(2L, UUID.randomUUID(), new OrganisasjonsnummerDto(orgnummer), new FødselsnummerDto(fødselsnummer),
            LocalDate.now(), LocalDate.now(), ForespørselStatusDto.UNDER_BEHANDLING, YtelseTypeDto.FORELDREPENGER, LocalDateTime.now());
        var response2 = new ForespørselResponse(3L, UUID.randomUUID(), new OrganisasjonsnummerDto(orgnummer), new FødselsnummerDto(fødselsnummer),
            LocalDate.now(), LocalDate.now(), ForespørselStatusDto.UTGÅTT, YtelseTypeDto.SVANGERSKAPSPENGER, LocalDateTime.now());

        when(fpinntektsmeldingKlient.hentForespørsler(new ForespørselFilterRequest(new OrganisasjonsnummerDto(orgnummer),
            null,
            null,
            null,
            null,
            null,
            1L))).thenReturn(
            List.of(response1, response2));
        var forespørsler = fpinntektsmeldingTjeneste.hentForespørsler(orgnummer, null, null, null, null, null, 1L);
        assertThat(forespørsler).hasSize(2);
        var forespørsel1 = forespørsler.stream().filter(f -> f.ytelseType().equals(YtelseType.FORELDREPENGER)).findFirst().orElseThrow();
        var forespørsel2 = forespørsler.stream().filter(f -> f.ytelseType().equals(YtelseType.SVANGERSKAPSPENGER)).findFirst().orElseThrow();

        assertThat(forespørsel1.orgnummer().orgnr()).isEqualTo(orgnummer);
        assertThat(forespørsel1.status()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(forespørsel1.fødselsnummer()).isEqualTo(fødselsnummer);

        assertThat(forespørsel2.orgnummer().orgnr()).isEqualTo(orgnummer);
        assertThat(forespørsel2.status()).isEqualTo(ForespørselStatus.UTGÅTT);
        assertThat(forespørsel2.fødselsnummer()).isEqualTo(fødselsnummer);

    }

    @Test
    void skal_mappe_status_fra_hentInntektsmelding_respons() {
        var uuid = UUID.randomUUID();
        var response = lagHentInntektsmeldingResponse(uuid, no.nav.foreldrepenger.inntektsmelding.felles.InntektsmeldingStatusDto.VENTER_VURDERING);
        when(fpinntektsmeldingKlient.hentInntektsmelding(uuid)).thenReturn(response);

        var inntektsmelding = fpinntektsmeldingTjeneste.hentInntektsmelding(uuid);

        assertThat(inntektsmelding.status()).isEqualTo(InntektsmeldingStatus.VENTER_VURDERING);
    }

    @Test
    void skal_mappe_null_status_fra_hentInntektsmelding_respons() {
        var uuid = UUID.randomUUID();
        var response = lagHentInntektsmeldingResponse(uuid, null);
        when(fpinntektsmeldingKlient.hentInntektsmelding(uuid)).thenReturn(response);

        var inntektsmelding = fpinntektsmeldingTjeneste.hentInntektsmelding(uuid);

        assertThat(inntektsmelding.status()).isNull();
    }

    private HentInntektsmeldingResponse lagHentInntektsmeldingResponse(UUID uuid,
                                                                        no.nav.foreldrepenger.inntektsmelding.felles.InntektsmeldingStatusDto status) {
        return new HentInntektsmeldingResponse(
            null,
            uuid,
            UUID.randomUUID(),
            new FødselsnummerDto("12345678901"),
            YtelseTypeDto.FORELDREPENGER,
            new OrganisasjonsnummerDto("999999999"),
            new KontaktpersonDto("Ola Nordmann", "12345678"),
            LocalDate.now(),
            BigDecimal.valueOf(50000),
            LocalDateTime.now(),
            BigDecimal.valueOf(30000),
            null,
            new AvsenderSystemDto("TestSystem", "1.0"),
            List.of(),
            List.of(),
            List.of(),
            status);
    }

    @Test
    void skal_sende_inntektsmelding_med_foreldrepenger() {
        var orgnummer = "999999999";
        var fødselsnummer = "12345678901";
        var uuid = UUID.randomUUID();
        var forespørsel = new Forespørsel(null, uuid, new Organisasjonsnummer(orgnummer), fødselsnummer,
            LocalDate.now(), LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.FORELDREPENGER, LocalDateTime.now());
        var inntektsmeldingRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.FORELDREPENGER,
            new InntektsmeldingRequest.InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            new InntektsmeldingRequest.Refusjon(BigDecimal.valueOf(25000.00), List.of()),
            List.of(),
            new InntektsmeldingRequest.Kontaktinformasjon("Kontaktperson", "12345678"),
            new InntektsmeldingRequest.Avsender("TestSystem", "1.0.0")
        );
        var responseUuid = UUID.randomUUID();
        var responseResultat = new SendInntektsmeldingResponse(true, responseUuid,
            no.nav.foreldrepenger.inntektsmelding.felles.InntektsmeldingStatusDto.GODKJENT,null);
        when(fpinntektsmeldingKlient.sendInntektsmelding(any())).thenReturn(responseResultat);

        var response = fpinntektsmeldingTjeneste.sendInntektsmelding(inntektsmeldingRequest, forespørsel);

        assertThat(response).isNotNull();
        verify(fpinntektsmeldingKlient).sendInntektsmelding(any());
    }

    @Test
    void skal_sende_inntektsmelding_med_bortfalt_naturalytelse() {
        var orgnummer = "777777777";
        var fødselsnummer = "11111111111";
        var uuid = UUID.randomUUID();
        var forespørsel = new Forespørsel(null, uuid, new Organisasjonsnummer(orgnummer), fødselsnummer,
            LocalDate.now(), LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.FORELDREPENGER, LocalDateTime.now());
        var bortfaltNaturalytelse = new InntektsmeldingRequest.Naturalytelse(
            InntektsmeldingRequest.Naturalytelse.Naturalytelsetype.ElektroniskKommunikasjon,
            BigDecimal.valueOf(500.00),
            LocalDate.now(),
            LocalDate.now().plusDays(10)
        );
        var inntektsmeldingRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.FORELDREPENGER,
            new InntektsmeldingRequest.InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            null,
            List.of(bortfaltNaturalytelse),
            new InntektsmeldingRequest.Kontaktinformasjon("Kontaktperson","12345678"),
            new InntektsmeldingRequest.Avsender("TestSystem", "1.0.0")
        );
        var responseUuid = UUID.randomUUID();
        when(fpinntektsmeldingKlient.sendInntektsmelding(any())).thenReturn(new SendInntektsmeldingResponse(true, responseUuid,
            no.nav.foreldrepenger.inntektsmelding.felles.InntektsmeldingStatusDto.GODKJENT,null));

        var response = fpinntektsmeldingTjeneste.sendInntektsmelding(inntektsmeldingRequest, forespørsel);

        assertThat(response).isNotNull();
        assertThat(responseUuid).isNotNull();
        assertThat(response.status()).isEqualTo(InntektsmeldingStatusDto.GODKJENT);
        verify(fpinntektsmeldingKlient).sendInntektsmelding(any());
    }

    @Test
    void skal_sende_inntektsmelding_med_endringsaarsaker() {
        var orgnummer = "666666666";
        var fødselsnummer = "22222222222";
        var uuid = UUID.randomUUID();
        var forespørsel = new Forespørsel(null, uuid, new Organisasjonsnummer(orgnummer), fødselsnummer,
            LocalDate.now(), LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.FORELDREPENGER, LocalDateTime.now());
        var endringsårsak = new InntektsmeldingRequest.InntektInfo.Endringsaarsak(
            InntektsmeldingRequest.InntektInfo.Endringsaarsak.EndringsaarsakType.Permisjon,
            LocalDate.now(),
            LocalDate.now().plusDays(5),
            LocalDate.now().minusDays(1)
        );
        var inntektsmeldingRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.FORELDREPENGER,
            new InntektsmeldingRequest.InntektInfo(BigDecimal.valueOf(25000.00), List.of(endringsårsak)),
            new InntektsmeldingRequest.Refusjon(BigDecimal.valueOf(25000.00), List.of()),
            List.of(),
            new InntektsmeldingRequest.Kontaktinformasjon("Kontaktperson", "12345678"),
            new InntektsmeldingRequest.Avsender("TestSystem", "1.0.0")
        );
        var responseUuid = UUID.randomUUID();
        var responseResultat = new SendInntektsmeldingResponse(true, responseUuid,
            no.nav.foreldrepenger.inntektsmelding.felles.InntektsmeldingStatusDto.GODKJENT,null);
        when(fpinntektsmeldingKlient.sendInntektsmelding(any())).thenReturn(responseResultat);

        var response = fpinntektsmeldingTjeneste.sendInntektsmelding(inntektsmeldingRequest, forespørsel);

        assertThat(response).isNotNull();
        assertThat(response).isEqualTo(responseResultat);
        verify(fpinntektsmeldingKlient).sendInntektsmelding(any());
    }

    @Test
    void skal_sende_inntektsmelding_med_flere_refusjonsperioder() {
        var orgnummer = "555555555";
        var fødselsnummer = "33333333333";
        var uuid = UUID.randomUUID();
        var forespørsel = new Forespørsel(null, uuid, new Organisasjonsnummer(orgnummer), fødselsnummer,
            LocalDate.now(), LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.FORELDREPENGER, LocalDateTime.now());
        var refusjoner =
            new InntektsmeldingRequest.Refusjon(BigDecimal.valueOf(25000.00), List.of(
                new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(20000),LocalDate.now().plusDays(10)),
                new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(15000), LocalDate.now().plusDays(20)))
            );
        var inntektsmeldingRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.FORELDREPENGER,
            new InntektsmeldingRequest.InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            refusjoner,
            List.of(),
            new InntektsmeldingRequest.Kontaktinformasjon("Kontaktperson", "12345678"),
            new InntektsmeldingRequest.Avsender("TestSystem", "1.0.0")
        );
        var responseUuid = UUID.randomUUID();
        var responseResultat = new SendInntektsmeldingResponse(true, responseUuid,
            no.nav.foreldrepenger.inntektsmelding.felles.InntektsmeldingStatusDto.GODKJENT, null);
        when(fpinntektsmeldingKlient.sendInntektsmelding(any())).thenReturn(responseResultat);

        var response = fpinntektsmeldingTjeneste.sendInntektsmelding(inntektsmeldingRequest, forespørsel);

        assertThat(response).isNotNull();
        assertThat(response).isEqualTo(responseResultat);
        verify(fpinntektsmeldingKlient).sendInntektsmelding(any());
    }
}
