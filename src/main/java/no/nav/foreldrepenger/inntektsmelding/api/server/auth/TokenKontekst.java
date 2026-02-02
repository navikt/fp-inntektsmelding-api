package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.SikkerhetContext;

public class TokenKontekst implements Kontekst {
    private String uuid;
    private String konsumentId;

    public TokenKontekst(String uuid, String konsumentId) {
        this.uuid = uuid;
        this.konsumentId = konsumentId;
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
}
