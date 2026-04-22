package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.integrasjoner.FpinntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.api.server.auth.Tilgang;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.foreldrepenger.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseType;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InntektsmeldingRestTest {
    @Mock
    private FpinntektsmeldingTjeneste fpinntektsmeldingTjeneste;
    @Mock
    private Tilgang tilgang;

    private InntektsmeldingRest inntektsmeldingRest;

    @BeforeEach
    void setUp() {
        inntektsmeldingRest = new InntektsmeldingRest(fpinntektsmeldingTjeneste, tilgang);
    }

    @Test
    void skal_sende_inntektsmelding_med_success() {
        // Arrange
        var orgnummer = "999999999";
        var fødselsnummer = "12345678901";
        var forespørselUuid = UUID.randomUUID();
        var responseUuid = UUID.randomUUID();

        var forespørsel = new Forespørsel(forespørselUuid, new Organisasjonsnummer(orgnummer), fødselsnummer,
            LocalDate.now(), LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.FORELDREPENGER,
            LocalDateTime.now());

        var inntektsmeldingRequest = new InntektsmeldingRequest(
            forespørselUuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.FORELDREPENGER,
            new InntektsmeldingRequest.Kontaktperson("Kontaktperson", "12345678"),
            new BigDecimal("25000.00"),
            List.of(new InntektsmeldingRequest.Refusjon(LocalDate.now(), new BigDecimal("25000.00"))),
            List.of(),
            List.of(),
            new InntektsmeldingRequest.AvsenderSystem("TestSystem", "1.0.0")
        );

        when(fpinntektsmeldingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(forespørsel);
        when(fpinntektsmeldingTjeneste.sendInntektsmelding(any(), any()))
            .thenReturn(new SendInntektsmeldingResponse(true, responseUuid, "OK"));

        // Act
        var response = inntektsmeldingRest.sendInntektsmelding(inntektsmeldingRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(responseUuid);
    }

    @Test
    void skal_returnere_feil_når_forespørsel_ikke_finnes() {
        // Arrange
        var forespørselUuid = UUID.randomUUID();

        var inntektsmeldingRequest = new InntektsmeldingRequest(
            forespørselUuid,
            "12345678901",
            LocalDate.now(),
            YtelseType.FORELDREPENGER,
            new InntektsmeldingRequest.Kontaktperson("Kontaktperson", "12345678"),
            new BigDecimal("25000.00"),
            List.of(new InntektsmeldingRequest.Refusjon(LocalDate.now(), new BigDecimal("25000.00"))),
            List.of(),
            List.of(),
            new InntektsmeldingRequest.AvsenderSystem("TestSystem", "1.0.0")
        );

        when(fpinntektsmeldingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(null);

        // Act
        var response = inntektsmeldingRest.sendInntektsmelding(inntektsmeldingRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        var errorResponse = (ErrorResponse) response.getEntity();
        assertThat(errorResponse.feilmelding()).isEqualTo(EksponertFeilmelding.TOM_FORESPOERSEL.getTekst() + ": " + forespørselUuid);
    }
}
