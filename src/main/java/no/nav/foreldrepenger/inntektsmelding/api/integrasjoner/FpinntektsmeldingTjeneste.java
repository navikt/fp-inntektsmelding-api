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
        var response = fpinntektsmeldingKlient.hentForespørsel(forespørselUuid);
        return mapResponseTilDomeneobjekt(response);
    }

    public List<Forespørsel> hentForespørsler(String orgnr,
                                              String fnr,
                                              StatusDto status,
                                              YtelseTypeDto ytelseType,
                                              LocalDate fom,
                                              LocalDate tom) {
        var filter = new ForespørselFilterRequest(new OrganisasjonsnummerDto(orgnr), fnr == null ? null : new FødselsnummerDto(fnr),
            status == null ? null : KodeverkMapper.mapApiStatusTilForespørselStatus(status),
            ytelseType,
            fom,
            tom);
        var response = fpinntektsmeldingKlient.hentForespørsler(filter);
        return response.stream().map(this::mapResponseTilDomeneobjekt).toList();
    }

    private Forespørsel mapResponseTilDomeneobjekt(ForespørselResponse response) {
        return new Forespørsel(response.forespørselUuid(),
            response.orgnummer(),
            response.fødselsnummer(),
            response.førsteUttaksdato(),
            response.skjæringstidspunkt(),
            response.status(),
            response.ytelseType(),
            response.opprettetTid());
    }


}
