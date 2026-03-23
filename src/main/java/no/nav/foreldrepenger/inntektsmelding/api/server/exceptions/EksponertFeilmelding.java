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
    UGYLDIG_PERIODE("Oppgitt forespørselsperiode er ugyldig, fom kan ikke være etter tom"),
    MISMATCH_ORGNR("Organisasjonsnummer fra token og organisasjonsnummer fra etterspurt forespørsel matcher ikke"),
    MISMATCH_FØRSTE_UTTAKSDATO("Første uttaksdato fra inntektsmelding og første uttaksdato fra etterspurt forespørsel matcher ikke"),
    MISMATCH_SKJÆRINGSTIDSPUNKT("Skjæringstidspunkt fra inntektsmelding og skjæringstidspunkt fra etterspurt forespørsel matcher ikke"),
    MISMATCH_YTELSE("Ytelse fra inntektsmelding og ytelse fra etterspurt forespørsel matcher ikke"),
    UGYLDIG_FORESPØRSEL("Det er ikke tillatt å sende inn en inntektsmelding på en forkastet forespørsel."),
    LIK_FOM_REFUSJON("Det er ikke tillatt med like fra datoer for refusjon"),
    REFUSJON_FOM_LIK_STARTDATO("Refusjonslisten må inneholde en fra dato som starter på startdato for permisjonen"),
    UGYLDIG_FRA_DATO_LISTE("Fra datoene i listen må være sammenhengende"),
    FRA_DATO_ETTER_TOM("Fra dato kan ikke være etter til dato"),
    OVERLAPP_I_PERIODER("Perioder kan ikke overlappe"),
    ÅRSAK_KREVER_FRA_DATO("Endringsårskene Ny stilling, Ny stillingsprosent og Varig lønndsendring krever at det oppgis en fra dato"),
    ÅRSAK_KREVER_FRA_OG_TIL_DATO("Endringsårskene Ferie, Permisjon, Permittering og Sykefravær krever at det oppgis en fra dato og en til dato"),
    KREVER_FRA_OG_BLE_KJENT_DATO("Endringsårsaken Tariffendring krever at fra dato og ble kjent dato er oppgitt"),
    FRA_DATO_FØR_STARTDATO("Fra dato må være før startdato for endringsårsak Varig lønnsendring"),
    DUPLIKATER_IKKE_TILATT(
        "Duplikate endringsårsker er ikke tillatt for årsakene: Ny stilling, Ny stillingsprosent, Varig lønnsendring, bonus, tariffendring, ferietrekk eller utbetaling av feriepenger, nyansatt, mangelfull rapportering a-ordning, inntekt ikke rapportert enda a-ordning"),
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
