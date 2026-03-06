package no.nav.foreldrepenger.inntektsmelding.api.typer;

public class KodeverkMapper {

    private KodeverkMapper() {
        // Skjuler default konstruktør
    }

    public static ForespørselStatus mapApiStatusTilForespørselStatus(Status status) {
        return switch (status) {
            case AKTIV -> ForespørselStatus.UNDER_BEHANDLING;
            case BESVART -> ForespørselStatus.FERDIG;
            case FORKASTET -> ForespørselStatus.UTGÅTT;
        };
    }

    public static Status mapForespørselStatusTilApiStatus(ForespørselStatus forespørselStatus) {
        return switch (forespørselStatus) {
            case UTGÅTT -> Status.FORKASTET;
            case UNDER_BEHANDLING -> Status.AKTIV;
            case FERDIG -> Status.BESVART;
        };

    }
}
