package no.nav.foreldrepenger.inntektsmelding.api.server.exceptions;

public class Feilmelding {
    public static final String MANGLER_TOKEN = "Mangler token i header";
    public static final String UTGÅTT_TOKEN = "Oppgitt token er utgått";
    public static final String STANDARD_FEIL = "Noe feilet";
    public static final String FEIL_SCOPE = "Token inneholder ikke riktig scope for denne operasjonen";

    public static final String TOM_FORESPØRSEL = "Finner ikke forespørsel";
    public static final String MISSMATCH_ORGNR = "Organisasjonsummer fra token %s og organisasjonsnummer fra etterspurt forespørsel %s matcher ikke";
    public static final String IKKE_TILGANG_ALTINN = "Systemet har ikke registrert tilgang til organisasjonen i Altinn";
    public static final String FEIL_OPPSLAG_ALTINN = "Klarte ikke slå opp rettigheter i Altinn";
}
