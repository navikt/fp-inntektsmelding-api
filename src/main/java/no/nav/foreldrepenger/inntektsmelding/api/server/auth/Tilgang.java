package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import no.nav.foreldrepenger.inntektsmelding.api.typer.OrganisasjonsnummerDto;

public interface Tilgang {

    void sjekkAtSystemHarTilgangTilOrganisasjon(OrganisasjonsnummerDto orgnummerFraForespørsel);

}
