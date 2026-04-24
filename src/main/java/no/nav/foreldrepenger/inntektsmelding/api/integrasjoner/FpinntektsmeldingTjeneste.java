package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.inntektsmelding.Inntektsmelding;
import no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne.InntektsmeldingRequest;
import no.nav.foreldrepenger.inntektsmelding.api.typer.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.foreldrepenger.inntektsmelding.api.typer.StatusDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseType;
import no.nav.foreldrepenger.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.foreldrepenger.inntektsmelding.felles.BortfaltNaturalytelseDto;
import no.nav.foreldrepenger.inntektsmelding.felles.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.felles.EndringsårsakerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.KontaktpersonDto;
import no.nav.foreldrepenger.inntektsmelding.felles.NaturalytelsetypeDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.SøktRefusjonDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel.ForespørselFilterRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel.ForespørselResponse;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.InntektsmeldingFilterRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;

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
        return response != null ? mapResponseTilDomeneobjekt(response) : null;
    }

    public List<Forespørsel> hentForespørsler(String orgnr,
                                              String fnr,
                                              StatusDto status,
                                              YtelseType ytelseType,
                                              LocalDate fom,
                                              LocalDate tom) {
        var filter = new ForespørselFilterRequest(new OrganisasjonsnummerDto(orgnr), fnr == null ? null : new FødselsnummerDto(fnr),
            status == null ? null : KodeverkMapper.mapApiStatusTilForespørselStatus(status),
            ytelseType == null ? null : mapYtelseType(ytelseType),
            fom,
            tom);
        var response = fpinntektsmeldingKlient.hentForespørsler(filter);
        return response.stream().map(this::mapResponseTilDomeneobjekt).toList();
    }

    public Inntektsmelding hentInntektsmelding(UUID innsendingId) {
        var response = fpinntektsmeldingKlient.hentInntektsmelding(innsendingId);
        return mapInntektsmeldingResponseTilDomeneobjekt(response);
    }

    public List<Inntektsmelding> hentInntektsmeldinger(String orgnr,
                                                       String fnr,
                                                       UUID uuid,
                                                       YtelseType ytelseType,
                                                       LocalDate fom,
                                                       LocalDate tom) {
        var request = new InntektsmeldingFilterRequest(new OrganisasjonsnummerDto(orgnr),
            fnr == null ? null : new FødselsnummerDto(fnr),
            ytelseType == null ? null : mapYtelseType(ytelseType),
            uuid,
            fom,
            tom);
        var response = fpinntektsmeldingKlient.hentInntektsmeldinger(request);
        return response.stream().map(this::mapInntektsmeldingResponseTilDomeneobjekt).toList();
    }

    private Inntektsmelding mapInntektsmeldingResponseTilDomeneobjekt(HentInntektsmeldingResponse response) {
        return new Inntektsmelding(
            response.inntektsmeldingUuid(),
            response.fnr().fnr(),
            KodeverkMapper.mapTilDto(KodeverkMapper.mapYtelseType(response.ytelseType())),
            new Organisasjonsnummer(response.arbeidsgiver().orgnr()),
            new Inntektsmelding.Kontaktperson(response.kontaktperson().navn(), response.kontaktperson().telefonnummer()),
            response.startdato(),
            response.inntekt(),
            response.startdato(), // TODO: legg inn skjæringstidspunkt
            response.innsendtTidspunkt(),
            new Inntektsmelding.AvsenderSystem(response.avsenderSystem().systemNavn(), response.avsenderSystem().systemVersjon()),
            response.refusjonPrMnd(),
            response.opphørsdatoRefusjon(),
            response.refusjonsendringer().stream()
                .map(r -> new Inntektsmelding.Refusjon(r.fom(), r.beløp()))
                .toList(),
            response.bortfaltNaturalytelsePerioder().stream()
                .map(b -> new Inntektsmelding.BortfaltNaturalytelse(b.fom(),
                    b.tom(),
                    mapNaturalytelseTypeTilApiType(b.naturalytelsetype()),
                    b.beløp()))
                .toList(),
            response.endringAvInntektÅrsaker().stream()
                .map(e -> new Inntektsmelding.Endringsårsaker(mapEndringsårsakTilApiType(e.årsak()), e.fom(), e.tom(), e.bleKjentFom()))
                .toList()
        );
    }

    private no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto mapNaturalytelseTypeTilApiType(NaturalytelsetypeDto naturalytelsetype) {
        return switch (naturalytelsetype) {
            case ELEKTRISK_KOMMUNIKASJON -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.ELEKTRISK_KOMMUNIKASJON;
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS ->
                no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
            case LOSJI -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.LOSJI;
            case KOST_DOEGN -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.KOST_DOEGN;
            case BESØKSREISER_HJEMMET_ANNET -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.BESØKSREISER_HJEMMET_ANNET;
            case KOSTBESPARELSE_I_HJEMMET -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.KOSTBESPARELSE_I_HJEMMET;
            case RENTEFORDEL_LÅN -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.RENTEFORDEL_LÅN;
            case BIL -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.BIL;
            case KOST_DAGER -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.KOST_DAGER;
            case BOLIG -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.BOLIG;
            case SKATTEPLIKTIG_DEL_FORSIKRINGER ->
                no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.SKATTEPLIKTIG_DEL_FORSIKRINGER;
            case FRI_TRANSPORT -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.FRI_TRANSPORT;
            case OPSJONER -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.OPSJONER;
            case TILSKUDD_BARNEHAGEPLASS -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.TILSKUDD_BARNEHAGEPLASS;
            case ANNET -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.ANNET;
            case BEDRIFTSBARNEHAGEPLASS -> no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.BEDRIFTSBARNEHAGEPLASS;
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER ->
                no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS ->
                no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS;
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING ->
                no.nav.foreldrepenger.inntektsmelding.api.typer.NaturalytelsetypeDto.INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING;
        };
    }

    private no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto mapEndringsårsakTilApiType(EndringsårsakDto årsak) {
        return switch (årsak) {
            case PERMITTERING -> no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.PERMITTERING;
            case NY_STILLING -> no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.NY_STILLING;
            case NY_STILLINGSPROSENT -> no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.NY_STILLINGSPROSENT;
            case SYKEFRAVÆR -> no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.SYKEFRAVÆR;
            case BONUS -> no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.BONUS;
            case FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER ->
                no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER;
            case NYANSATT -> no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.NYANSATT;
            case MANGELFULL_RAPPORTERING_AORDNING ->
                no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.MANGELFULL_RAPPORTERING_AORDNING;
            case INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING ->
                no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING;
            case TARIFFENDRING -> no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.TARIFFENDRING;
            case FERIE -> no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.FERIE;
            case VARIG_LØNNSENDRING -> no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.VARIG_LØNNSENDRING;
            case PERMISJON -> no.nav.foreldrepenger.inntektsmelding.api.typer.EndringsårsakDto.PERMISJON;
        };
    }


    public SendInntektsmeldingResponse sendInntektsmelding(InntektsmeldingRequest inntektsmeldingRequest, Forespørsel forespørsel) {
        var inntektsmeldingRequestDto = new SendInntektsmeldingRequest(
            forespørsel.forespørselUuid(),
            new FødselsnummerDto(forespørsel.fødselsnummer()),
            new OrganisasjonsnummerDto(forespørsel.orgnummer().orgnr()),
            inntektsmeldingRequest.startdato(),
            mapYtelseType(inntektsmeldingRequest.ytelse()),
            mapKontaktPersonDto(inntektsmeldingRequest.kontaktperson()),
            inntektsmeldingRequest.inntekt(),
            mapRefusjonDto(inntektsmeldingRequest.refusjon()),
            mapNaturalYtelseDto(inntektsmeldingRequest.bortfaltNaturalytelsePerioder()),
            mapEndringsårsakerDto(inntektsmeldingRequest.endringAvInntektÅrsaker()),
            new AvsenderSystemDto(inntektsmeldingRequest.avsenderSystem().navn(),
                inntektsmeldingRequest.avsenderSystem().versjon())
        );

        return fpinntektsmeldingKlient.sendInntektsmelding(inntektsmeldingRequestDto);
    }

    private List<EndringsårsakerDto> mapEndringsårsakerDto(List<InntektsmeldingRequest.Endringsårsaker> endringsårsaker) {
        return endringsårsaker.stream()
            .map(e -> new EndringsårsakerDto(mapÅrsakType(e.årsak()), e.fom(), e.tom(), e.bleKjentFom()))
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

    private List<BortfaltNaturalytelseDto> mapNaturalYtelseDto(List<InntektsmeldingRequest.BortfaltNaturalytelse> bortfalteNaturalYtelser) {
        return bortfalteNaturalYtelser.stream()
            .map(b -> new BortfaltNaturalytelseDto(b.fom(), b.tom(), mapNaturalYtelseType(b.naturalytelsetype()), b.beløp()))
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
            case BIL -> NaturalytelsetypeDto.BIL;
            case KOST_DAGER -> NaturalytelsetypeDto.KOST_DAGER;
            case BOLIG -> NaturalytelsetypeDto.BOLIG;
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> NaturalytelsetypeDto.SKATTEPLIKTIG_DEL_FORSIKRINGER;
            case FRI_TRANSPORT -> NaturalytelsetypeDto.FRI_TRANSPORT;
            case OPSJONER -> NaturalytelsetypeDto.OPSJONER;
            case TILSKUDD_BARNEHAGEPLASS -> NaturalytelsetypeDto.TILSKUDD_BARNEHAGEPLASS;
            case ANNET -> NaturalytelsetypeDto.ANNET;
            case BEDRIFTSBARNEHAGEPLASS -> NaturalytelsetypeDto.BEDRIFTSBARNEHAGEPLASS;
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS;
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> NaturalytelsetypeDto.INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING;
        };
    }

    private List<SøktRefusjonDto> mapRefusjonDto(List<InntektsmeldingRequest.Refusjon> refusjon) {
        return refusjon.stream()
            .map(r -> new SøktRefusjonDto(r.fom(), r.beløp()))
            .toList();
    }


    private no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto mapYtelseType(YtelseType ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto.SVANGERSKAPSPENGER;
        };
    }

    private KontaktpersonDto mapKontaktPersonDto(InntektsmeldingRequest.Kontaktperson kontaktperson) {
        return new KontaktpersonDto(kontaktperson.navn(), kontaktperson.telefonnummer());
    }

    private Forespørsel mapResponseTilDomeneobjekt(ForespørselResponse response) {
        return new Forespørsel(response.forespørselUuid(),
            new Organisasjonsnummer(response.orgnummer().orgnr()),
            response.fødselsnummer().fnr(),
            response.førsteUttaksdato(),
            response.skjæringstidspunkt(),
            KodeverkMapper.mapForespørselStatus(response.status()),
            KodeverkMapper.mapYtelseType(response.ytelseType()),
            response.opprettetTid());
    }


}
