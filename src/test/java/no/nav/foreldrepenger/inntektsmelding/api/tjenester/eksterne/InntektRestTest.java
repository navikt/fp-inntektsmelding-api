package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.inntekt.Inntekt;
import no.nav.foreldrepenger.inntektsmelding.api.inntekt.InntektDto;
import no.nav.foreldrepenger.inntektsmelding.api.integrasjoner.FpinntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.api.server.auth.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.foreldrepenger.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseType;

@ExtendWith(MockitoExtension.class)
class InntektRestTest {
    @Mock
    private FpinntektsmeldingTjeneste fpinntektsmeldingTjeneste;
    @Mock
    private Tilgang tilgang;

    private InntektRest inntektRest;

    @BeforeEach
    void setUp() {
        inntektRest = new InntektRest(fpinntektsmeldingTjeneste, tilgang);
    }

    @Test
    void skal_hente_inntekt() {
        var orgnummer = "999999999";
        var forespørselUuid = UUID.randomUUID();
        var forespørsel = new Forespørsel(1L, forespørselUuid, new Organisasjonsnummer(orgnummer), "11111111111", LocalDate.now(), LocalDate.now(),
            ForespørselStatus.UNDER_BEHANDLING, YtelseType.FORELDREPENGER, LocalDateTime.now());
        var inntekt = new Inntekt(Map.of(YearMonth.of(2025, 3), BigDecimal.valueOf(30000)), BigDecimal.valueOf(30000));

        when(fpinntektsmeldingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(forespørsel);
        when(fpinntektsmeldingTjeneste.hentInntekt(forespørselUuid)).thenReturn(inntekt);

        var response = inntektRest.hentInntekt(forespørselUuid.toString());

        assertThat(response.getStatus()).isEqualTo(200);
        var dto = (InntektDto) response.getEntity();
        assertThat(dto.gjennomsnittAvMaaneder()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(dto.inntektPerMaaned()).containsEntry(YearMonth.of(2025, 3), BigDecimal.valueOf(30000));
        verify(tilgang).sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(orgnummer));
    }

    @Test
    void skal_returnere_404_når_forespørsel_ikke_finnes() {
        var forespørselUuid = UUID.randomUUID();
        when(fpinntektsmeldingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(null);

        var response = inntektRest.hentInntekt(forespørselUuid.toString());

        assertThat(response.getStatus()).isEqualTo(404);
        var error = (ErrorResponse) response.getEntity();
        assertThat(error.feilkode()).isEqualTo(EksponertFeilmelding.TOM_FORESPOERSEL.name());
    }
}
