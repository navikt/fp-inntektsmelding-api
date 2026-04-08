package no.nav.foreldrepenger.inntektsmelding.api.typer;

public class KodeverkMapper {

    private KodeverkMapper() {
        // Skjuler default konstruktør
    }

    public static no.nav.foreldrepenger.inntektsmelding.felles.ForespørselStatusDto mapApiStatusTilForespørselStatus(StatusDto status) {
        return switch (status) {
            case AKTIV -> no.nav.foreldrepenger.inntektsmelding.felles.ForespørselStatusDto.UNDER_BEHANDLING;
            case BESVART -> no.nav.foreldrepenger.inntektsmelding.felles.ForespørselStatusDto.FERDIG;
            case FORKASTET -> no.nav.foreldrepenger.inntektsmelding.felles.ForespørselStatusDto.UTGÅTT;
        };
    }

    public static StatusDto mapTilDto(ForespørselStatus forespørselStatus) {
        return switch (forespørselStatus) {
            case UTGÅTT -> StatusDto.FORKASTET;
            case UNDER_BEHANDLING -> StatusDto.AKTIV;
            case FERDIG -> StatusDto.BESVART;
        };
    }

    public static YtelseTypeDto mapTilDto(YtelseType ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
        };
    }

    public static YtelseType mapYtelseType(no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto ytelseTypeDto) {
        return switch (ytelseTypeDto) {
            case FORELDREPENGER -> YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseType.SVANGERSKAPSPENGER;
        };
    }

    public static ForespørselStatus mapForespørselStatus(no.nav.foreldrepenger.inntektsmelding.felles.ForespørselStatusDto status) {
        return switch (status) {
            case UNDER_BEHANDLING -> ForespørselStatus.UNDER_BEHANDLING;
            case FERDIG -> ForespørselStatus.FERDIG;
            case UTGÅTT -> ForespørselStatus.UTGÅTT;
        };
    }
}
