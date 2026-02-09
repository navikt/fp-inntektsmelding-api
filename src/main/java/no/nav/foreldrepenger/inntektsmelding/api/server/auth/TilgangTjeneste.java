package no.nav.foreldrepenger.inntektsmelding.api.server.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.server.auth.altinnPdp.PdpKlient;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

public class TilgangTjeneste implements Tilgang {
    private static final Logger LOG = LoggerFactory.getLogger(TilgangTjeneste.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");
    private static final Environment ENV = Environment.current();

    @Override
    public void sjekkAtSystemHarTilgangTilOrganisasjon(Organisasjonsnummer orgnummerFraForespørsel) {
        var orgnummerFraKontekst = hentOrgnrFraKontekst();
        var systemId = hentSystemIdFraKontekst();
        if (!orgnummerFraKontekst.equals(orgnummerFraForespørsel)) {
            SECURE_LOG.warn("Kontekst har ikke samme orgnummer som forespørsel. "
                + "Orgnummer fra kontekst var {} og orgnummer fra forespørsel var {}", orgnummerFraKontekst, orgnummerFraForespørsel);
            throw ikkeTilgang(String.format("Missmatch mellom organisasjonsummer fra token %s og organisasjonsnummer fra etterspurt forespørsel %s", orgnummerFraKontekst, orgnummerFraForespørsel));
        }
        var ressurs = ENV.getRequiredProperty("altinn.tre.inntektsmelding.ressurs");

        try {
            var harTilgang = PdpKlient.instance().systemHarRettighetForOrganisasjon(systemId, orgnummerFraForespørsel.orgnr(), ressurs);
            if (!harTilgang) {
                // TODO Skriv en feilmelding
                throw ikkeTilgang("");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Organisasjonsnummer hentOrgnrFraKontekst() {
        if (KontekstHolder.getKontekst() instanceof TokenKontekst tk) {
            return tk.getOrganisasjonNummer();
        }
        throw ikkeTilgang("Har ikke gyldig token-kontekst");
    }

    private String hentSystemIdFraKontekst() {
        if (KontekstHolder.getKontekst() instanceof TokenKontekst tk) {
            return tk.getSystemUserId();
        }
        throw ikkeTilgang("Har ikke gyldig token-kontekst");
    }

    private static ManglerTilgangException ikkeTilgang(String begrunnelse) {
        LOG.info("Fikk ikke tilgang pga: {}", begrunnelse);
        return new ManglerTilgangException("IM-00403", String.format("Mangler tilgang til tjenesten. %s", begrunnelse));
    }

}
