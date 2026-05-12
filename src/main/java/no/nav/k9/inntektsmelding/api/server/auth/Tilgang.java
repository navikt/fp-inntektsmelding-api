package no.nav.k9.inntektsmelding.api.server.auth;

import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;

public interface Tilgang {

    void sjekkAtSystemHarTilgangTilOrganisasjon(Organisasjonsnummer orgnummerFraForespørsel);

}
