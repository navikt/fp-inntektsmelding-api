package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.typer.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.api.typer.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.StatusDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

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
                                              StatusDto status,
                                              YtelseTypeDto ytelseType,
                                              LocalDate fom,
                                              LocalDate tom) {
        return fpinntektsmeldingKlient.hentForespørsler(new OrganisasjonsnummerDto(orgnr),
            new FødselsnummerDto(fnr),
            KodeverkMapper.mapApiStatusTilForespørselStatus(status),
            ytelseType,
            fom,
            tom);
    }

}
