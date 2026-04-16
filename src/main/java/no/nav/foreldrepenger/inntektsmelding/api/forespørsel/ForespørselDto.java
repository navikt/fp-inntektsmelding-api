package no.nav.foreldrepenger.inntektsmelding.api.forespørsel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.api.typer.StatusDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

public record ForespørselDto(UUID forespoerselId, String orgnr, String fnr, LocalDate foersteUttaksdato,
                             LocalDate inntektsdato, StatusDto status, YtelseTypeDto ytelseType, LocalDateTime opprettetTid) {

}
