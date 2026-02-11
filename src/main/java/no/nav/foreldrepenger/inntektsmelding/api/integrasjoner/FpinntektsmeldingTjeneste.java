package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import jakarta.enterprise.context.Dependent;

import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;

import java.util.UUID;

@Dependent
public class FpinntektsmeldingTjeneste {
    private FpinntektsmeldingKlient fpinntektsmeldingKlient;

    FpinntektsmeldingTjeneste() {
            // for CDI proxy
    }

    @Inject
    public FpinntektsmeldingTjeneste(FpinntektsmeldingKlient fpinntektsmeldingKlient) {
        this.fpinntektsmeldingKlient = fpinntektsmeldingKlient;
    }

        public Forespørsel hentForespørsel(UUID forespørselUuid) {
            return fpinntektsmeldingKlient.hentForespørsel(forespørselUuid);
        }
}
