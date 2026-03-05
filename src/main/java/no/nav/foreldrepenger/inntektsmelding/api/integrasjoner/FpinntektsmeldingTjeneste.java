package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import jakarta.validation.Valid;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;

import no.nav.foreldrepenger.inntektsmelding.api.typer.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.api.typer.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Status;

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

    public List<Forespørsel> hentForespørsler(String orgnr,
                                              String fnr,
                                              Status status,
                                              LocalDate fom,
                                              LocalDate tom) {
        return fpinntektsmeldingKlient.hentForespørsler(new OrganisasjonsnummerDto(orgnr),
            new FødselsnummerDto(fnr),
            KodeverkMapper.mapApiStatusTilForespørselStatus(status),
            fom,
            tom);
    }

}
