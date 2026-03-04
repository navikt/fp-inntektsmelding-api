package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class InntektsmeldingRequestSerializationTest {


    @Test
    void skal_serialisere_til_json() throws Exception {
        // Arrange
        var request = lagTestRequest();

        // Act
        var json = DefaultJsonMapper.toJson(request);

        // Assert
        assertThat(json).contains("\"navn\":\"Test Kontaktperson\"");
        assertThat(json).contains("\"telefonnummer\":\"12345678\"");
        assertThat(json).contains("\"ytelse\":\"FORELDREPENGER\"");
        assertThat(json).contains("\"inntekt\":25000.00");
    }

    @Test
    void skal_deserialisere_fra_json() throws Exception {
        // Arrange
        var request = lagTestRequest();
        var json = DefaultJsonMapper.toJson(request);

        // Act
        var deserializedRequest = DefaultJsonMapper.fromJson(json, InntektsmeldingRequest.class);

        // Assert
        assertThat(deserializedRequest.foresporselUuid()).isEqualTo(request.foresporselUuid());
        assertThat(deserializedRequest.fødselsnummer()).isEqualTo(request.fødselsnummer());
        assertThat(deserializedRequest.startdato()).isEqualTo(request.startdato());
        assertThat(deserializedRequest.ytelse()).isEqualTo(request.ytelse());
        assertThat(deserializedRequest.inntekt()).isEqualTo(request.inntekt());
        assertThat(deserializedRequest.avsenderSystem().navn()).isEqualTo(request.avsenderSystem().navn());
        assertThat(deserializedRequest.avsenderSystem().versjon()).isEqualTo(request.avsenderSystem().versjon());
    }

    @Test
    void skal_serialisere_og_deserialisere_med_alle_felt() throws Exception {
        // Arrange
        var uuid = UUID.randomUUID();
        var fødselsnummer = "12345678901";
        var startdato = LocalDate.of(2024, 1, 15);
        var kontaktperson = new InntektsmeldingRequest.Kontaktperson("Ola Nordmann", "98765432");
        var avsenderSystem = new InntektsmeldingRequest.AvsenderSystem("SAP", "1.0.0");
        var refusjon = List.of(
            new InntektsmeldingRequest.Refusjon(LocalDate.of(2024, 1, 1), new BigDecimal("25000.00"))
        );
        var bortfaltNaturalytelse = List.of(
            new InntektsmeldingRequest.BortfaltNaturalytelse(
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 28),
                InntektsmeldingRequest.BortfaltNaturalytelse.Naturalytelsetype.ELEKTRISK_KOMMUNIKASJON,
                new BigDecimal("500.00")
            )
        );
        var endringsårsaker = List.of(
            new InntektsmeldingRequest.Endringsårsaker(
                InntektsmeldingRequest.Endringsårsaker.Endringsårsak.PERMISJON,
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 15),
                LocalDate.of(2024, 2, 15)
            )
        );

        var originalRequest = new InntektsmeldingRequest(
            uuid, fødselsnummer, startdato, InntektsmeldingRequest.YtelseType.FORELDREPENGER,
            kontaktperson, new BigDecimal("25000.00"), refusjon, bortfaltNaturalytelse,
            endringsårsaker, avsenderSystem
        );

        // Act
        var json = DefaultJsonMapper.toJson(originalRequest);
        var deserializedRequest = DefaultJsonMapper.fromJson(json, InntektsmeldingRequest.class);

        // Assert
        assertThat(deserializedRequest).isEqualTo(originalRequest);
        assertThat(deserializedRequest.foresporselUuid()).isEqualTo(uuid);
        assertThat(deserializedRequest.kontaktperson().navn()).isEqualTo("Ola Nordmann");
        assertThat(deserializedRequest.refusjon()).hasSize(1);
        assertThat(deserializedRequest.bortfaltNaturalytelsePerioder()).hasSize(1);
        assertThat(deserializedRequest.endringAvInntektÅrsaker()).hasSize(1);
    }

    @Test
    void skal_serialisere_naturaltelse_typer() throws Exception {
        // Arrange
        var request = lagTestRequest();

        // Act
        var json = DefaultJsonMapper.toJson(request);

        // Assert
        assertThat(json).contains("\"naturalytelsetype\":\"ELEKTRISK_KOMMUNIKASJON\"");
    }

    @Test
    void skal_serialisere_endringsårsak_typer() throws Exception {
        // Arrange
        var request = lagTestRequest();

        // Act
        var json = DefaultJsonMapper.toJson(request);

        // Assert
        assertThat(json).contains("\"årsak\":\"PERMISJON\"");
    }

    @Test
    void skal_deserialisere_avsender_system() throws Exception {
        // Arrange
        var json = """
            {
              "navn": "TestSystem",
              "versjon": "2.5.0"
            }
            """;

        // Act
        var avsenderSystem = DefaultJsonMapper.fromJson(json, InntektsmeldingRequest.AvsenderSystem.class);

        // Assert
        assertThat(avsenderSystem.navn()).isEqualTo("TestSystem");
        assertThat(avsenderSystem.versjon()).isEqualTo("2.5.0");
    }

    private InntektsmeldingRequest lagTestRequest() {
        return new InntektsmeldingRequest(
            UUID.randomUUID(),
            "12345678901",
            LocalDate.of(2024, 1, 15),
            InntektsmeldingRequest.YtelseType.FORELDREPENGER,
            new InntektsmeldingRequest.Kontaktperson("Test Kontaktperson", "12345678"),
            new BigDecimal("25000.00"),
            List.of(new InntektsmeldingRequest.Refusjon(LocalDate.of(2024, 1, 1), new BigDecimal("25000.00"))),
            List.of(new InntektsmeldingRequest.BortfaltNaturalytelse(
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 28),
                InntektsmeldingRequest.BortfaltNaturalytelse.Naturalytelsetype.ELEKTRISK_KOMMUNIKASJON,
                new BigDecimal("500.00")
            )),
            List.of(new InntektsmeldingRequest.Endringsårsaker(
                InntektsmeldingRequest.Endringsårsaker.Endringsårsak.PERMISJON,
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 15),
                LocalDate.of(2024, 2, 15)
            )),
            new InntektsmeldingRequest.AvsenderSystem("SAP", "1.0.0")
        );
    }
}

