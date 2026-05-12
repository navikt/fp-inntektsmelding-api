package no.nav.k9.inntektsmelding.api.server.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Feilrespons returnert ved feil")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @Schema(description = "Maskinlesbar feilkode", example = "FEIL_KODE") String feilkode,
    @Schema(description = "Beskrivelse av feilen", example = "Beskrivelse av feilen") String feilmelding,
    @Schema(description = "ForespørselId eller InntektsmeldingId på den som feilet", nullable = true) String referanseId) {

    public ErrorResponse(String feilkode, String feilmelding) {
        this(feilkode, feilmelding, null);
    }
}



