package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.foreldrepenger.inntektsmelding.api.typer.OrganisasjonsnummerDto;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.SikkerhetContext;

public class TokenKontekst implements Kontekst {
    private String uuid;
    private String konsumentId; // lps sitt orgnummer
    private String organisasjonNummer; // arbeidsgivers orgnummer (knyttet til forespørsel)
    private String systemUserId; // Id på avsendersystemet, registrert i Altinn

    public TokenKontekst(String uuid, String konsumentId, String organisasjonNummer, String systemUserId) {
        this.uuid = uuid;
        this.konsumentId = konsumentId;
        this.organisasjonNummer = organisasjonNummer;
        this.systemUserId = systemUserId;
    }

    @Override
    public SikkerhetContext getContext() {
        return SikkerhetContext.REQUEST;
    }

    @Override
    public String getUid() {
        return uuid;
    }

    @Override
    public String getKompaktUid() {
        return uuid;
    }

    @Override
    public IdentType getIdentType() {
        return IdentType.Systemressurs;
    }

    @Override
    public String getKonsumentId() {
        return konsumentId;
    }

    public OrganisasjonsnummerDto getOrganisasjonNummer() {
        return vaskOrgnummer(organisasjonNummer);
    }

    private OrganisasjonsnummerDto vaskOrgnummer(String orgnrString) {
        var vasketOrgnr = orgnrString.substring(orgnrString.indexOf(":") + 1);
        return new OrganisasjonsnummerDto(vasketOrgnr);
    }

    public String getSystemUserId() {
        return systemUserId;
    }
}
