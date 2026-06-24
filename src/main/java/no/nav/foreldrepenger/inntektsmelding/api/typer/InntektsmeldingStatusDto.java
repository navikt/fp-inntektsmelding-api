package no.nav.foreldrepenger.inntektsmelding.api.typer;

/** Statusmeldinger som eksponeres ut om LPS-ene hvis A-inntekt er nede. */

public enum InntektsmeldingStatusDto {
    /** Inntektsmeldingen er mottatt og venter på vurdering. */
    MOTTATT,
    /** Inntektsmeldingen er kontrollert og godkjent mot a-inntekt. */
    GODKJENT,
    /** Inntektsmeldingen avviker fra a-inntekt og ble avvist. */
    AVVIST,
}
