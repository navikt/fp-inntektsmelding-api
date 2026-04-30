package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Feilrespons returnert ved feil")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @Schema(description = "Maskinlesbar feilkode", example = "TOM_FORESPOERSEL") String feilkode,
    @Schema(description = "Beskrivelse av feilen", example = "Finner ikke forespørsel") String feilmelding,
    @Schema(description = "Referanse-ID for feilsøking (callId)", nullable = true) String feilreferanse) {

    public ErrorResponse(String feilkode, String feilmelding) {
        this(feilkode, feilmelding, null);
    }
}



