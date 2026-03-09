package no.nav.foreldrepenger.inntektsmelding.api.typer;

public class KodeverkMapper {

    private KodeverkMapper() {
        // Skjuler default konstruktør
    }

    public static ForespørselStatus mapApiStatusTilForespørselStatus(StatusDto status) {
        return switch (status) {
            case AKTIV -> ForespørselStatus.UNDER_BEHANDLING;
            case BESVART -> ForespørselStatus.FERDIG;
            case FORKASTET -> ForespørselStatus.UTGÅTT;
        };
    }

    public static StatusDto mapForespørselStatusTilApiStatus(ForespørselStatus forespørselStatus) {
        return switch (forespørselStatus) {
            case UTGÅTT -> StatusDto.FORKASTET;
            case UNDER_BEHANDLING -> StatusDto.AKTIV;
            case FERDIG -> StatusDto.BESVART;
        };
    }
}
