package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.api.typer.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

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
    void skal_bestemt_forespørsel() {
        var orgnummer = "999999999";
        var uuid = UUID.randomUUID();
        var response = new ForespørselResponse(uuid, new OrganisasjonsnummerDto(orgnummer), "123",
            LocalDate.now(), LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseTypeDto.FORELDREPENGER, LocalDateTime.now());
        when(fpinntektsmeldingKlient.hentForespørsel(uuid)).thenReturn(response);
        var forespørsel = fpinntektsmeldingTjeneste.hentForespørsel(uuid);
        assertThat(forespørsel.orgnummer().orgnr()).isEqualTo(orgnummer);
        assertThat(forespørsel.ytelseType()).isEqualTo(response.ytelseType());
        assertThat(forespørsel.fødselsnummer()).isEqualTo(response.fødselsnummer());
    }

    @Test
    void skal_hente_tom_liste_forespørsler() {
        var orgnummer = "999999999";
        when(fpinntektsmeldingKlient.hentForespørsler(new ForespørselFilterRequest(new OrganisasjonsnummerDto(orgnummer),
            null,
            null,
            null,
            null,
            null))).thenReturn(
            List.of());
        var forespørsler = fpinntektsmeldingTjeneste.hentForespørsler(orgnummer, null, null, null, null, null);
        assertThat(forespørsler).isEmpty();
    }

    @Test
    void skal_hente_liste_forespørsler() {
        var orgnummer = "999999999";
        var response1 = new ForespørselResponse(UUID.randomUUID(), new OrganisasjonsnummerDto(orgnummer), "123",
            LocalDate.now(), LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseTypeDto.FORELDREPENGER, LocalDateTime.now());
        var response2 = new ForespørselResponse(UUID.randomUUID(), new OrganisasjonsnummerDto(orgnummer), "321",
            LocalDate.now(), LocalDate.now(), ForespørselStatus.UTGÅTT, YtelseTypeDto.SVANGERSKAPSPENGER, LocalDateTime.now());

        when(fpinntektsmeldingKlient.hentForespørsler(new ForespørselFilterRequest(new OrganisasjonsnummerDto(orgnummer),
            null,
            null,
            null,
            null,
            null))).thenReturn(
            List.of(response1, response2));
        var forespørsler = fpinntektsmeldingTjeneste.hentForespørsler(orgnummer, null, null, null, null, null);
        assertThat(forespørsler).hasSize(2);
        var forespørsel1 = forespørsler.stream().filter(f -> f.ytelseType().equals(YtelseTypeDto.FORELDREPENGER)).findFirst().orElseThrow();
        var forespørsel2 = forespørsler.stream().filter(f -> f.ytelseType().equals(YtelseTypeDto.SVANGERSKAPSPENGER)).findFirst().orElseThrow();

        assertThat(forespørsel1.orgnummer().orgnr()).isEqualTo(orgnummer);
        assertThat(forespørsel1.status()).isEqualTo(response1.status());
        assertThat(forespørsel1.fødselsnummer()).isEqualTo(response1.fødselsnummer());

        assertThat(forespørsel2.orgnummer().orgnr()).isEqualTo(orgnummer);
        assertThat(forespørsel2.status()).isEqualTo(response2.status());
        assertThat(forespørsel2.fødselsnummer()).isEqualTo(response2.fødselsnummer());

    }
}
