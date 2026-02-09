package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;

public interface Tilgang {

    void sjekkAtSystemHarTilgangTilOrganisasjon(Organisasjonsnummer orgnummerFraForespørsel);

}
