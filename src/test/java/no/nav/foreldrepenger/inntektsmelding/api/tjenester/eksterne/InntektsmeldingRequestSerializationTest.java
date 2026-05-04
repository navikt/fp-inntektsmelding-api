package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseType;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class InntektsmeldingRequestSerializationTest {


    @Test
    void skal_serialisere_til_json()  {
        // Arrange
        var request = lagTestRequest();

        // Act
        var json = DefaultJsonMapper.toJson(request);

        // Assert
        assertThat(json).contains("\"kontaktinformasjon\":\"Test Kontaktperson\"");
        assertThat(json).contains("\"arbeidsgiverTlf\":\"12345678\"");
        assertThat(json).contains("\"ytelse\":\"FORELDREPENGER\"");
        assertThat(json).contains("\"inntekt\":{\"beloepPerMaaned\":25000.0,\"endringAarsaker\":[{\"aarsak\":\"PERMISJON\",\"fom\":\"2024-03-01\",\"tom\":\"2024-03-15\",\"gjelderFra\":\"2024-02-15\"}]}");
    }

    @Test
    void skal_deserialisere_fra_json() throws Exception {
        // Arrange
        var request = lagTestRequest();
        var json = DefaultJsonMapper.toJson(request);

        // Act
        var deserializedRequest = DefaultJsonMapper.fromJson(json, InntektsmeldingRequest.class);

        // Assert
        assertThat(deserializedRequest.foresporselId()).isEqualTo(request.foresporselId());
        assertThat(deserializedRequest.foedselsnummer()).isEqualTo(request.foedselsnummer());
        assertThat(deserializedRequest.startdato()).isEqualTo(request.startdato());
        assertThat(deserializedRequest.ytelse()).isEqualTo(request.ytelse());
        assertThat(deserializedRequest.inntekt()).isEqualTo(request.inntekt());
        assertThat(deserializedRequest.avsenderSystem().systemNavn()).isEqualTo(request.avsenderSystem().systemNavn());
        assertThat(deserializedRequest.avsenderSystem().systemVersjon()).isEqualTo(request.avsenderSystem().systemVersjon());
    }

    @Test
    void skal_serialisere_og_deserialisere_med_alle_felt() throws Exception {
        // Arrange
        var uuid = UUID.randomUUID();
        var fødselsnummer = "12345678901";
        var startdato = LocalDate.of(2024, 1, 15);
        var kontaktperson = "Ola Nordmann";
        var arbeidsgiverTlf = "98765432";
        var avsenderSystem = new InntektsmeldingRequest.Avsender("SAP", "1.0.0");
        var refusjon = new InntektsmeldingRequest.Refusjon( BigDecimal.valueOf(25000.00), List.of());
        var bortfaltNaturalytelse = List.of(
            new InntektsmeldingRequest.Naturalytelse(
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 28),
                InntektsmeldingRequest.Naturalytelse.Naturalytelsetype.ELEKTRISK_KOMMUNIKASJON,
                new BigDecimal("500.00")
            )
        );
        var endringsårsaker = List.of(
            new InntektsmeldingRequest.InntektInfo.Endringsårsak(
                InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMISJON,
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 15),
                LocalDate.of(2024, 2, 15)
            )
        );

        var originalRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            startdato,
            YtelseType.FORELDREPENGER,
            new InntektsmeldingRequest.InntektInfo(BigDecimal.valueOf(25000.00), endringsårsaker),
            refusjon,
            bortfaltNaturalytelse,
            kontaktperson,
            arbeidsgiverTlf,
            avsenderSystem
        );

        // Act
        var json = DefaultJsonMapper.toJson(originalRequest);
        var deserializedRequest = DefaultJsonMapper.fromJson(json, InntektsmeldingRequest.class);

        // Assert
        assertThat(deserializedRequest).isEqualTo(originalRequest);
        assertThat(deserializedRequest.foresporselId()).isEqualTo(uuid);
        assertThat(deserializedRequest.kontaktinformasjon()).isEqualTo("Ola Nordmann");
        assertThat(deserializedRequest.arbeidsgiverTlf()).isEqualTo( "98765432");
        assertThat(deserializedRequest.naturalytelser()).hasSize(1);
        assertThat(deserializedRequest.inntekt().endringAarsaker()).hasSize(1);
    }

    @Test
    void skal_serialisere_naturaltelse_typer() throws Exception {
        // Arrange
        var request = lagTestRequest();

        // Act
        var json = DefaultJsonMapper.toJson(request);

        // Assert
        assertThat(json).contains("\"naturalytelse\":\"ELEKTRISK_KOMMUNIKASJON\"");
    }

    @Test
    void skal_serialisere_endringsårsak_typer() throws Exception {
        // Arrange
        var request = lagTestRequest();

        // Act
        var json = DefaultJsonMapper.toJson(request);

        // Assert
        assertThat(json).contains("\"aarsak\":\"PERMISJON\"");
    }

    @Test
    void skal_deserialisere_avsender_system() throws Exception {
        // Arrange
        var json = """
            {
              "systemNavn": "TestSystem",
              "systemVersjon": "2.5.0"
            }
            """;

        // Act
        var avsenderSystem = DefaultJsonMapper.fromJson(json, InntektsmeldingRequest.Avsender.class);

        // Assert
        assertThat(avsenderSystem.systemNavn()).isEqualTo("TestSystem");
        assertThat(avsenderSystem.systemVersjon()).isEqualTo("2.5.0");
    }

    private InntektsmeldingRequest lagTestRequest() {
        return new InntektsmeldingRequest(
            UUID.randomUUID(),
            "12345678901",
            LocalDate.of(2024, 1, 15),
            YtelseType.FORELDREPENGER,
            new InntektsmeldingRequest.InntektInfo(BigDecimal.valueOf(25000.00), List.of(new InntektsmeldingRequest.InntektInfo.Endringsårsak(
                InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMISJON,
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 15),
                LocalDate.of(2024, 2, 15)
            ))),
            new InntektsmeldingRequest.Refusjon(BigDecimal.valueOf(25000.00), List.of()),
            List.of(new InntektsmeldingRequest.Naturalytelse(
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 28),
                InntektsmeldingRequest.Naturalytelse.Naturalytelsetype.ELEKTRISK_KOMMUNIKASJON,
                new BigDecimal("500.00"))),
            "Test Kontaktperson",
            "12345678",
            new InntektsmeldingRequest.Avsender("SAP", "1.0.0")
        );
    }
}

