package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel.ForespørselFilterRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel.ForespørselResponse;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.InntektsmeldingFilterRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;
import no.nav.vedtak.konfig.Tid;

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
                                              LocalDate tom,
                                              Long fraLoepenr) {
        var filter = new ForespørselFilterRequest(new OrganisasjonsnummerDto(orgnr), fnr == null ? null : new FødselsnummerDto(fnr),
            status == null ? null : KodeverkMapper.mapApiStatusTilForespørselStatus(status),
            ytelseType == null ? null : mapYtelseType(ytelseType),
            fom,
            tom,
            fraLoepenr);
        var response = fpinntektsmeldingKlient.hentForespørsler(filter);
        return response.stream()
            .map(this::mapResponseTilDomeneobjekt)
            .sorted(Comparator.comparingLong(Forespørsel::loepenr))
            .toList();
    }

    public Inntektsmelding hentInntektsmelding(UUID innsendingId) {
        var response = fpinntektsmeldingKlient.hentInntektsmelding(innsendingId);
        return response == null ? null : mapInntektsmeldingResponseTilDomeneobjekt(response);
    }

    public List<Inntektsmelding> hentInntektsmeldinger(String orgnr,
                                                       String fnr,
                                                       UUID uuid,
                                                       YtelseType ytelseType,
                                                       LocalDate fom,
                                                       LocalDate tom,
                                                       Long fraLoepenr) {
        var request = new InntektsmeldingFilterRequest(new OrganisasjonsnummerDto(orgnr),
            fnr == null ? null : new FødselsnummerDto(fnr),
            ytelseType == null ? null : mapYtelseType(ytelseType),
            uuid,
            fom,
            tom,
            fraLoepenr);
        var response = fpinntektsmeldingKlient.hentInntektsmeldinger(request);
        return response.stream()
            .map(this::mapInntektsmeldingResponseTilDomeneobjekt)
            .sorted(Comparator.comparingLong(Inntektsmelding::loepenr))
            .toList();
    }

    private Inntektsmelding mapInntektsmeldingResponseTilDomeneobjekt(HentInntektsmeldingResponse response) {
        return new Inntektsmelding(
            response.loepenr(),
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
                .toList(),
            response.status() != null ? KodeverkMapper.mapInntektsmeldingStatus(response.status()) : null
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
            mapKontaktPersonDto(inntektsmeldingRequest.kontaktinformasjon()),
            inntektsmeldingRequest.inntekt().beloepPerMaaned(),
            mapRefusjonDto(inntektsmeldingRequest.refusjon(), inntektsmeldingRequest.startdato()),
            mapNaturalYtelseDto(inntektsmeldingRequest.naturalytelser()),
            mapEndringsårsakerDto(inntektsmeldingRequest.inntekt().endringAarsaker()),
            new AvsenderSystemDto(inntektsmeldingRequest.avsender().systemNavn(),
                inntektsmeldingRequest.avsender().systemVersjon())
        );

        return fpinntektsmeldingKlient.sendInntektsmelding(inntektsmeldingRequestDto);
    }

    private List<EndringsårsakerDto> mapEndringsårsakerDto(List<InntektsmeldingRequest.InntektInfo.Endringsaarsak> endringsaarsak) {
        return endringsaarsak.stream()
            .map(e -> new EndringsårsakerDto(mapÅrsakType(e.aarsak()), e.fom(), e.tom(), e.gjelderFra()))
            .toList();
    }

    private EndringsårsakDto mapÅrsakType(InntektsmeldingRequest.InntektInfo.Endringsaarsak.EndringsaarsakType årsakType) {
        return switch (årsakType) {
            case Permittering -> EndringsårsakDto.PERMITTERING;
            case NyStilling -> EndringsårsakDto.NY_STILLING;
            case NyStillingsprosent -> EndringsårsakDto.NY_STILLINGSPROSENT;
            case Sykefravaer -> EndringsårsakDto.SYKEFRAVÆR;
            case Bonus -> EndringsårsakDto.BONUS;
            case Ferietrekk -> EndringsårsakDto.FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER;
            case Nyansatt -> EndringsårsakDto.NYANSATT;
            case MangelfullRapporteringAordning -> EndringsårsakDto.MANGELFULL_RAPPORTERING_AORDNING;
            case InntektIkkeRapportertEndaAordning -> EndringsårsakDto.INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING;
            case Tariffendring -> EndringsårsakDto.TARIFFENDRING;
            case Ferie -> EndringsårsakDto.FERIE;
            case VarigLoennsendring -> EndringsårsakDto.VARIG_LØNNSENDRING;
            case Permisjon -> EndringsårsakDto.PERMISJON;
        };
    }

    private List<BortfaltNaturalytelseDto> mapNaturalYtelseDto(List<InntektsmeldingRequest.Naturalytelse> naturalYtelser) {

        return naturalYtelser.stream()
            .map(b -> new BortfaltNaturalytelseDto(b.bortfallerFra(), b.bortfallerTil() != null ? b.bortfallerTil() : Tid.TIDENES_ENDE, mapNaturalYtelseType(b.naturalytelse()), b.beloepPerMaaned()))
            .toList();
    }

    private NaturalytelsetypeDto mapNaturalYtelseType(InntektsmeldingRequest.Naturalytelse.Naturalytelsetype naturalytelsetype) {
        return switch (naturalytelsetype) {
            case ElektroniskKommunikasjon -> NaturalytelsetypeDto.ELEKTRISK_KOMMUNIKASJON;
            case AksjerGrunnfondsbevisTilUnderkurs -> NaturalytelsetypeDto.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
            case Losji -> NaturalytelsetypeDto.LOSJI;
            case KostDoegn -> NaturalytelsetypeDto.KOST_DOEGN;
            case BesoeksreiserHjemmetAnnet -> NaturalytelsetypeDto.BESØKSREISER_HJEMMET_ANNET;
            case KostbesparelseIHjemmet -> NaturalytelsetypeDto.KOSTBESPARELSE_I_HJEMMET;
            case RentefordelLaan -> NaturalytelsetypeDto.RENTEFORDEL_LÅN;
            case Bil -> NaturalytelsetypeDto.BIL;
            case KostDager -> NaturalytelsetypeDto.KOST_DAGER;
            case Bolig -> NaturalytelsetypeDto.BOLIG;
            case SkattepliktigDelForsikringer -> NaturalytelsetypeDto.SKATTEPLIKTIG_DEL_FORSIKRINGER;
            case FriTransport -> NaturalytelsetypeDto.FRI_TRANSPORT;
            case Opsjoner -> NaturalytelsetypeDto.OPSJONER;
            case TilskuddBarnehageplass -> NaturalytelsetypeDto.TILSKUDD_BARNEHAGEPLASS;
            case Annet -> NaturalytelsetypeDto.ANNET;
            case Bedriftsbarnehageplass -> NaturalytelsetypeDto.BEDRIFTSBARNEHAGEPLASS;
            case YrkebilTjenestligbehovKilometer -> NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case YrkebilTjenestligbehovListepris -> NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS;
            case InnbetalingTilUtenlandskPensjonsordning -> NaturalytelsetypeDto.INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING;
        };
    }

    private List<SøktRefusjonDto> mapRefusjonDto(InntektsmeldingRequest.Refusjon refusjon, LocalDate startdato) {
        if (refusjon == null) {
            return List.of();
        }
        List<SøktRefusjonDto> søktRefusjonDtoListe = new ArrayList<>();
        søktRefusjonDtoListe.add(new SøktRefusjonDto(startdato, refusjon.beloepPerMaaned()));
        refusjon.endringer().forEach(r -> søktRefusjonDtoListe.add(new SøktRefusjonDto(r.stardato(), r.beloepPerMaaned())));
        return søktRefusjonDtoListe;
    }


    private YtelseTypeDto mapYtelseType(YtelseType ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
        };
    }

    private KontaktpersonDto mapKontaktPersonDto(InntektsmeldingRequest.Kontaktinformasjon kontaktinformasjon) {
        return new KontaktpersonDto(kontaktinformasjon.arbeidsgiverNavn(), kontaktinformasjon.arbeidsgiverTlf());
    }

    private Forespørsel mapResponseTilDomeneobjekt(ForespørselResponse response) {
        return new Forespørsel(response.loepenr(),
            response.forespørselUuid(),
            new Organisasjonsnummer(response.orgnummer().orgnr()),
            response.fødselsnummer().fnr(),
            response.førsteUttaksdato(),
            response.skjæringstidspunkt(),
            KodeverkMapper.mapForespørselStatus(response.status()),
            KodeverkMapper.mapYtelseType(response.ytelseType()),
            response.opprettetTid());
    }


}
