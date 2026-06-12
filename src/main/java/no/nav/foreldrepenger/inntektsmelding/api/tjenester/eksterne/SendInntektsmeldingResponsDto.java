package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.api.typer.InntektsmeldingStatusDto;

public record SendInntektsmeldingResponsDto(UUID inntektsmeldingId, InntektsmeldingStatusDto status) {
}
