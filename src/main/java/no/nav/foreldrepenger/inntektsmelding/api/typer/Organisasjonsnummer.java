package no.nav.foreldrepenger.inntektsmelding.api.typer;

public record Organisasjonsnummer(String orgnr) {

    public Organisasjonsnummer(String orgnr) {
        var gyldigOrgnr = OrganisasjonsNummerValidator.erGyldig(orgnr);
        if (!gyldigOrgnr) {
            throw new IllegalArgumentException("Ugyldig organisasjonsnummer: " + orgnr);
        }
        this.orgnr = orgnr;
    }

    @Override
    public String toString() {
        return "Organisasjonsnummer[" + "orgnr=" + "*".repeat(orgnr.length() - 4) + orgnr.substring(orgnr.length() - 4) + "]";
    }
}
