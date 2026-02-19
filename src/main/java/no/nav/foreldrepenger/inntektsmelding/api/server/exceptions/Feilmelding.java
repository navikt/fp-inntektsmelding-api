package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

public enum Feilmelding {

    // Tilgangsfeil
    MANGLER_TOKEN("Mangler token i header"),
    UTGÅTT_TOKEN("Oppgitt token er utgått"),
    FEIL_SCOPE("Token inneholder ikke riktig scope for denne operasjonen"),
    IKKE_TILGANG_ALTINN("Systemet har ikke registrert tilgang til organisasjonen i Altinn"),
    FEIL_OPPSLAG_ALTINN("Klarte ikke slå opp rettigheter i Altinn"),

    // Valideringsfeil,
    TOM_FORESPØRSEL("Finner ikke forespørsel"),
    MISSMATCH_ORGNR("Organisasjonsummer fra token og organisasjonsnummer fra etterspurt forespørsel matcher ikke"),

    // Default
    STANDARD_FEIL("Noe feilet");

    private final String verdi;

    Feilmelding(String verdi) {
        this.verdi = verdi;
    }

    public String getVerdi() {
        return verdi;
    }
}
