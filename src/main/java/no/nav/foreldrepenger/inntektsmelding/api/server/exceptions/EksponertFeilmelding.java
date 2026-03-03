package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

public enum EksponertFeilmelding {

    // Tilgangsfeil
    MANGLER_TOKEN("Mangler token i header"),
    UTGÅTT_TOKEN("Oppgitt token er utgått"),
    UGYLDIG_TOKEN("Oppgitt token er ugyldig"),
    FEIL_SCOPE("Token inneholder ikke riktig scope for denne operasjonen"),
    IKKE_TILGANG_ALTINN("Systemet har ikke registrert tilgang til organisasjonen i Altinn"),
    FEIL_OPPSLAG_ALTINN("Klarte ikke slå opp rettigheter i Altinn"),

    // Valideringsfeil
    SERIALISERINGSFEIL("Serialiseringsfeil"),
    TOM_FORESPØRSEL("Finner ikke forespørsel"),
    MISMATCH_ORGNR("Organisasjonsummer fra token og organisasjonsnummer fra etterspurt forespørsel matcher ikke"),
    MISMATCH_FØRSTE_UTTAKSDATO("Første uttaksdato fra inntektsmelding og første uttaksdato fra etterspurt forespørsel matcher ikke"),
    MISMATCH_SKJÆRINGSTIDSPUNKT("Skjæringstidspunkt fra inntektsmelding og skjæringstidspunkt fra etterspurt forespørsel matcher ikke"),
    MISMATCH_YTELSE("Ytelse fra inntektsmelding og ytelse fra etterspurt forespørsel matcher ikke"),
    MISMATCH_AKTØR_ID("AktørId fra inntektsmelding og AktørId fra etterspurt forespørsel matcher ikke"),
    UGYLDIG_FORESPØRSEL("Det er ikke tillatt å sende inn en inntektsmelding på en forkastet forespørsel."),

    // Default
    STANDARD_FEIL("Noe feilet");

    private final String verdi;

    EksponertFeilmelding(String verdi) {
        this.verdi = verdi;
    }

    public String getVerdi() {
        return verdi;
    }
}
