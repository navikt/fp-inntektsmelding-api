package no.nav.foreldrepenger.inntektsmelding.api.forespørsel;

import no.nav.foreldrepenger.inntektsmelding.api.typer.KodeverkMapper;

public class ForespørselMapper {

    private ForespørselMapper() {
        // Skjuler default
    }

    private static ForespørselDto maptilDto(Forespørsel forespørsel) {
        return new ForespørselDto(forespørsel.forespørselUuid(),
            forespørsel.orgnummer().orgnr(),
            forespørsel.fødselsnummer(),
            forespørsel.førsteUttaksdato(),
            forespørsel.skjæringstidspunkt(),
            KodeverkMapper.mapForespørselStatusTilApiStatus(forespørsel.status()),
            forespørsel.ytelseType());
    }

}
