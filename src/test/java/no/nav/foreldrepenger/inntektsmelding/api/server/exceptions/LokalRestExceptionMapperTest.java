package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ch.qos.logback.classic.Level;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.log.util.MemoryAppender;
import no.nav.vedtak.server.rest.RestServerFeilUtils;

@Execution(ExecutionMode.SAME_THREAD)
class LokalRestExceptionMapperTest {

    private static MemoryAppender logSniffer;
    private static MemoryAppender mapperLogSniffer;

    private final LokalRestExceptionMapper exceptionMapper = new LokalRestExceptionMapper();

    @BeforeEach
    void setUp() {
        logSniffer = MemoryAppender.sniff(RestServerFeilUtils.class);
        mapperLogSniffer = MemoryAppender.sniff(LokalRestExceptionMapper.class);
    }

    @AfterEach
    void afterEach() {
        logSniffer.reset();
        mapperLogSniffer.reset();
    }

    @ParameterizedTest
    @EnumSource(value = EksponertFeilmelding.class,
        names = {"MANGLER_TOKEN", "UTGAATT_TOKEN", "UGYLDIG_TOKEN", "FEIL_SCOPE", "IKKE_TILGANG_ALTINN"})
    void skalLoggeTilgangsfeilSomInfo(EksponertFeilmelding feilmelding) {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);

        var feil = new InntektsmeldingAPIException(feilmelding, Response.Status.UNAUTHORIZED);
        try (var response = exceptionMapper.toResponse(feil)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
            assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
            assertThat(response.getEntity()).isInstanceOf(ErrorResponse.class);
            var feilDto = (ErrorResponse) response.getEntity();

            assertThat(feilDto.feilkode()).isEqualTo(feilmelding.name());
            assertThat(feilDto.feilmelding()).isEqualTo(feilmelding.getTekst());
            assertThat(mapperLogSniffer.getLoggedEvents()).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.INFO);
                assertThat(event.getFormattedMessage()).contains(feilmelding.name());
            });
            assertThat(logSniffer.getLoggedEvents()).isEmpty();
        }
    }

    @ParameterizedTest
    @EnumSource(value = EksponertFeilmelding.class, mode = EnumSource.Mode.EXCLUDE,
        names = {"MANGLER_TOKEN", "UTGAATT_TOKEN", "UGYLDIG_TOKEN", "FEIL_SCOPE", "IKKE_TILGANG_ALTINN"})
    void skalBeholdeWarnForAndreApiFeil(EksponertFeilmelding feilmelding) {
        var feil = new InntektsmeldingAPIException(feilmelding, Response.Status.INTERNAL_SERVER_ERROR);

        try (var response = exceptionMapper.toResponse(feil)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            assertThat(logSniffer.search(feilmelding.name(), Level.WARN)).hasSize(1);
            assertThat(mapperLogSniffer.getLoggedEvents()).isEmpty();
        }
    }

    @Test
    void skalMappeFunksjonellFeil() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);
        try (var response = exceptionMapper.toResponse(funksjonellFeil())) {
            assertThat(response.getEntity()).isInstanceOf(ErrorResponse.class);
            var feilDto = (ErrorResponse) response.getEntity();

            assertThat(feilDto.feilmelding()).isEqualTo(EksponertFeilmelding.TOM_FORESPOERSEL.getTekst());
        }
    }

    @Test
    void skalMappeVLException() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);
        try (var response = exceptionMapper.toResponse(new FunksjonellException("FPIMAPI-123456", "en teknisk feilmelding") {

        })) {
            assertThat(response.getEntity()).isInstanceOf(ErrorResponse.class);
            var feilDto = (ErrorResponse) response.getEntity();
            assertThat(feilDto.feilmelding()).isEqualTo(EksponertFeilmelding.STANDARD_FEIL.getTekst());
            assertThat(logSniffer.search("en teknisk feilmelding", Level.WARN)).hasSize(1);
        }
    }

    @Test
    void skalMappeWrappedGenerellFeil() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);
        var feilmelding = "en helt generell feil";
        var generellFeil = new RuntimeException(feilmelding);

        try (var response = exceptionMapper.toResponse(new TekniskException("KODE", "TEKST", generellFeil))) {
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isInstanceOf(ErrorResponse.class);
            var feilDto = (ErrorResponse) response.getEntity();

            assertThat(feilDto.feilmelding()).isEqualTo(EksponertFeilmelding.STANDARD_FEIL.getTekst());
            assertThat(logSniffer.search("TEKST", Level.WARN)).hasSize(1);
        }
    }

    @Test
    void skalMappeGenerellFeil() {
        var callId = MDCOperations.generateCallId();
        MDCOperations.putCallId(callId);
        var feilmelding = "en helt generell feil";
        RuntimeException generellFeil = new IllegalArgumentException(feilmelding);

        try (var response = exceptionMapper.toResponse(generellFeil)) {
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isInstanceOf(ErrorResponse.class);
            var feilDto = (ErrorResponse) response.getEntity();

            assertThat(feilDto.feilmelding()).isEqualTo(EksponertFeilmelding.STANDARD_FEIL.getTekst());
            assertThat(logSniffer.search(feilmelding, Level.WARN)).hasSize(1);
        }
    }

    private static InntektsmeldingAPIException funksjonellFeil() {
        return new InntektsmeldingAPIException(EksponertFeilmelding.TOM_FORESPOERSEL, Response.Status.INTERNAL_SERVER_ERROR);
    }
}
