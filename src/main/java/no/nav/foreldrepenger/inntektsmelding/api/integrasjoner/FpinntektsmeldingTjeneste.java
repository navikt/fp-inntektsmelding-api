package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne.InntektsmeldingRequest;
import no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto;
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

    public Response sendInntektsmelding(InntektsmeldingRequest inntektsmeldingRequest, Forespørsel forespørsel) {
        var inntektsmeldingRequestDto = new InntektsmeldingRequestDto(
            forespørsel.forespørselUuid(),
            forespørsel.fødselsnummer(),
            new OrganisasjonsnummerDto(forespørsel.orgnummer().orgnr()),
            inntektsmeldingRequest.startdato(),
            mapYtelseType(inntektsmeldingRequest.ytelse()),
            mapKontaktPersonDto(inntektsmeldingRequest.kontaktperson()),
            inntektsmeldingRequest.inntekt(),
            mapRefusjonDto(inntektsmeldingRequest.refusjon()),
            mapNaturalYtelseDto(inntektsmeldingRequest.bortfaltNaturalytelsePerioder()),
            mapEndringsårsakerDto(inntektsmeldingRequest.endringAvInntektÅrsaker()),
            new InntektsmeldingRequestDto.AvsenderSystemDto(inntektsmeldingRequest.avsenderSystem().navn(),
                inntektsmeldingRequest.avsenderSystem().versjon())
            );

       return fpinntektsmeldingKlient.sendInntektsmelding(inntektsmeldingRequestDto);
    }

    private List<InntektsmeldingRequestDto.EndringsårsakerDto> mapEndringsårsakerDto(List<InntektsmeldingRequest.Endringsårsaker> endringsårsaker) {
        return endringsårsaker.stream()
            .map(e -> new InntektsmeldingRequestDto.EndringsårsakerDto(mapÅrsakType(e.årsak()), e.fom(), e.tom(), e.bleKjentFom()))
            .toList();
    }

    private EndringsårsakDto mapÅrsakType(InntektsmeldingRequest.Endringsårsaker.Endringsårsak årsak) {
        return switch (årsak) {
            case PERMITTERING -> EndringsårsakDto.PERMITTERING;
            case NY_STILLING -> EndringsårsakDto.NY_STILLING;
            case NY_STILLINGSPROSENT -> EndringsårsakDto.NY_STILLINGSPROSENT;
            case SYKEFRAVÆR -> EndringsårsakDto.SYKEFRAVÆR;
            case BONUS -> EndringsårsakDto.BONUS;
            case FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER -> EndringsårsakDto.FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER;
            case NYANSATT -> EndringsårsakDto.NYANSATT;
            case MANGELFULL_RAPPORTERING_AORDNING -> EndringsårsakDto.MANGELFULL_RAPPORTERING_AORDNING;
            case INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING -> EndringsårsakDto.INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING;
            case TARIFFENDRING -> EndringsårsakDto.TARIFFENDRING;
            case FERIE -> EndringsårsakDto.FERIE;
            case VARIG_LØNNSENDRING -> EndringsårsakDto.VARIG_LØNNSENDRING;
            case PERMISJON -> EndringsårsakDto.PERMISJON;
        };
    }

    private List<InntektsmeldingRequestDto.BortfaltNaturalYtelseDto> mapNaturalYtelseDto(List<InntektsmeldingRequest.BortfaltNaturalytelse> bortfalteNaturalYtelser) {
        return bortfalteNaturalYtelser.stream()
            .map(b -> new InntektsmeldingRequestDto.BortfaltNaturalYtelseDto(b.fom(), b.tom(), mapNaturalYtelseType(b.naturalytelsetype()), b.beløp()))
            .toList();
    }

    private NaturalytelsetypeDto mapNaturalYtelseType(InntektsmeldingRequest.BortfaltNaturalytelse.Naturalytelsetype naturalytelsetype) {
        return switch (naturalytelsetype) {
            case ELEKTRISK_KOMMUNIKASJON -> NaturalytelsetypeDto.ELEKTRISK_KOMMUNIKASJON;
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> NaturalytelsetypeDto.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
            case LOSJI -> NaturalytelsetypeDto.LOSJI;
            case KOST_DOEGN -> NaturalytelsetypeDto.KOST_DOEGN;
            case BESØKSREISER_HJEMMET_ANNET -> NaturalytelsetypeDto.BESØKSREISER_HJEMMET_ANNET;
            case KOSTBESPARELSE_I_HJEMMET -> NaturalytelsetypeDto.KOSTBESPARELSE_I_HJEMMET;
            case RENTEFORDEL_LÅN -> NaturalytelsetypeDto.RENTEFORDEL_LÅN;
            case BIL ->  NaturalytelsetypeDto.BIL;
            case KOST_DAGER -> NaturalytelsetypeDto.KOST_DAGER;
            case BOLIG -> NaturalytelsetypeDto.BOLIG;
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> NaturalytelsetypeDto.SKATTEPLIKTIG_DEL_FORSIKRINGER;
            case FRI_TRANSPORT -> NaturalytelsetypeDto.FRI_TRANSPORT;
            case OPSJONER -> NaturalytelsetypeDto.OPSJONER;
            case TILSKUDD_BARNEHAGEPLASS -> NaturalytelsetypeDto.TILSKUDD_BARNEHAGEPLASS;
            case ANNET -> NaturalytelsetypeDto.ANNET;
            case BEDRIFTSBARNEHAGEPLASS -> NaturalytelsetypeDto.BEDRIFTSBARNEHAGEPLASS;
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> NaturalytelsetypeDto.INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING;
        };
    }

    private List<InntektsmeldingRequestDto.RefusjonDto> mapRefusjonDto(List<InntektsmeldingRequest.Refusjon> refusjon) {
        return refusjon.stream()
            .map(r -> new InntektsmeldingRequestDto.RefusjonDto(r.fom(), r.beløp()))
            .toList();
    }

    private YtelseTypeDto mapYtelseType(InntektsmeldingRequest.YtelseType ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
        };
    }

    private InntektsmeldingRequestDto.KontaktpersonDto mapKontaktPersonDto(InntektsmeldingRequest.Kontaktperson kontaktperson) {
         if (kontaktperson == null) {
            return null;
        }
        return new InntektsmeldingRequestDto.KontaktpersonDto(kontaktperson.navn(), kontaktperson.telefonnummer());
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
