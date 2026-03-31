package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.api.typer.ForespørselStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class InntektsmeldingValidererUtil {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingValidererUtil.class);

    private InntektsmeldingValidererUtil() {
        //skal ikke initialiseres
    }


    public static Optional<EksponertFeilmelding> validerInntektsmelding(InntektsmeldingRequest inntektsmeldingRequest, Forespørsel forespørsel) {
        var feilmeldingForespørsel = validerInntektsmeldingMotForespørsel(inntektsmeldingRequest, forespørsel);
        if (feilmeldingForespørsel.isPresent()) {
            return feilmeldingForespørsel;
        }

        var feilmeldingRefusjon = validerRefusjon(inntektsmeldingRequest.refusjon(), inntektsmeldingRequest.startdato());
        if (feilmeldingRefusjon.isPresent()) {
            return feilmeldingRefusjon;
        }

        var feilmeldingNaturalytelse = validerNaturalytelse(inntektsmeldingRequest.bortfaltNaturalytelsePerioder());
        if (feilmeldingNaturalytelse.isPresent()) {
            return feilmeldingNaturalytelse;
        }

        return validerEndringsårsaker(inntektsmeldingRequest.endringAvInntektÅrsaker(), inntektsmeldingRequest.startdato());
    }

    public static Optional<EksponertFeilmelding> validerInntektsmeldingMotForespørsel(InntektsmeldingRequest inntektsmeldingRequest,
                                                                                      Forespørsel forespørsel) {
        if (forespørsel.status() == ForespørselStatus.UTGÅTT) {
            LOG.warn("Forespørsel med uuid {} har status UTGÅTT, og kan ikke motta inntektsmelding.", forespørsel.forespørselUuid());
            return Optional.of(EksponertFeilmelding.UGYLDIG_FORESPØRSEL);
        }
        if (!inntektsmeldingRequest.startdato().equals(forespørsel.førsteUttaksdato())) {
            LOG.warn("Startdato fra inntektsmelding {} og første uttaksdato fra forespørsel {} matcher ikke.",
                inntektsmeldingRequest.startdato(),
                forespørsel.førsteUttaksdato());
            return Optional.of(EksponertFeilmelding.MISMATCH_FØRSTE_UTTAKSDATO);
        }
        if (!mapYtelseType(inntektsmeldingRequest.ytelse()).equals(forespørsel.ytelseType())) {
            LOG.warn("Ytelsetype fra inntektsmelding {} og ytelsetype fra forespørsel {} matcher ikke.",
                inntektsmeldingRequest.ytelse(),
                forespørsel.ytelseType());
            return Optional.of(EksponertFeilmelding.MISMATCH_YTELSE);
        }
        return Optional.empty();
    }

    private static YtelseTypeDto mapYtelseType(InntektsmeldingRequest.YtelseType ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
        };
    }

    public static Optional<EksponertFeilmelding> validerRefusjon(List<InntektsmeldingRequest.Refusjon> refusjon, LocalDate startdato) {
        if (refusjon == null) {
            return Optional.empty();
        }
        //Todo Avklare om vi skal sjekke på at refusjonsbeløp ikke kan være større enn inntekt dersom inntekt ikke er 0. Sykepenger gjør dette, men hos oss er det tillatt i portalen i dag hvis endringsårsak oppgis.
        // Det fins tilfeller hvor arbeidsgiver ønsker å gjøre dette.
        if (refusjon.stream().noneMatch(r -> r.fom().isEqual(startdato))) {
            LOG.info(
                "Refusjonslisten må inneholde en fra dato som starter på startdato for permisjonen. Startdato for permisjon {}, Refusjonslistens fra-datoer {}",
                startdato,
                refusjon.stream().map(InntektsmeldingRequest.Refusjon::fom).toList());
            return Optional.of(EksponertFeilmelding.UGYLDIG_FRA_DATO_LISTE);
        }

        var fomListe = refusjon.stream()
            .map(InntektsmeldingRequest.Refusjon::fom)
            .toList();
        var harDuplikateFoms = fomListe.size() > 1 && fomListe.size() != new java.util.HashSet<>(fomListe).size();
        if (harDuplikateFoms) {
            LOG.info("Refusjon har duplikate fom-datoer: {}", fomListe);
            return Optional.of(EksponertFeilmelding.LIK_FOM_REFUSJON);
        }
        return Optional.empty();
    }

    public static Optional<EksponertFeilmelding> validerNaturalytelse(List<InntektsmeldingRequest.BortfaltNaturalytelse> bortfaltNaturalytelsePerioder) {
        if (bortfaltNaturalytelsePerioder == null) {
            return Optional.empty();
        }

        var perioderMedFomOgTomDato = bortfaltNaturalytelsePerioder.stream()
            .filter(periode -> periode.fom() != null && periode.tom() != null)
            .toList();
        if (finnesOverlapp(perioderMedFomOgTomDato,
            InntektsmeldingRequest.BortfaltNaturalytelse::fom,
            InntektsmeldingRequest.BortfaltNaturalytelse::tom)) {
            LOG.info("Bortfalt naturalytelse har overlappende perioder");
            return Optional.of(EksponertFeilmelding.OVERLAPP_I_PERIODER);
        }

        if (perioderMedFomOgTomDato.stream().anyMatch(periode -> fraDatoEtterTom(periode.fom(), periode.tom()))) {
            LOG.info("Bortfalt naturalytelse har ugyldig periode. Fra dato er etter til dato for en eller flere perioder");
            return Optional.of(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
        }

        var perioderMedKunFomDato = bortfaltNaturalytelsePerioder.stream()
            .filter(periode -> periode.fom() != null && periode.tom() == null)
            .toList();

        boolean harDuplikater = perioderMedKunFomDato.size() != new HashSet<>(perioderMedKunFomDato).size();

        if (harDuplikater) {
            LOG.info("Bortfalt naturalytelse har duplikate fom-datoer for perioder uten tom-dato: {}",
                perioderMedKunFomDato.stream().map(InntektsmeldingRequest.BortfaltNaturalytelse::fom).toList());
            return Optional.of(EksponertFeilmelding.LIK_FOM_NATURALYTELSER);
        }
        return Optional.empty();
    }


    public static Optional<EksponertFeilmelding> validerEndringsårsaker(List<InntektsmeldingRequest.Endringsårsaker> oppgitteEndringsårsaker,
                                                                        LocalDate startdato) {
        // Todo Tariffendring skal kun være tilgjengelig dersom man endrer en IM, ikke for førstegangs-innsendelse
        if (oppgitteEndringsårsaker == null) {
            return Optional.empty();
        }

        var unikeÅrsakerListe = oppgitteEndringsårsaker.stream().map(InntektsmeldingRequest.Endringsårsaker::årsak)
            .filter(InntektsmeldingValidererUtil::skalÅrsakVæreUnik)
            .toList();

        boolean harDuplikater = unikeÅrsakerListe.size() != new HashSet<>(unikeÅrsakerListe).size();
        if (harDuplikater) {
            LOG.info("Det er oppgitt flere endringsårsaker av samme type: {}", unikeÅrsakerListe);
            return Optional.of(EksponertFeilmelding.DUPLIKATER_IKKE_TILATT);
        }

        var feilmeldingTariffendring = oppgitteEndringsårsaker.stream()
            .filter(årsak -> årsak.årsak() == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.TARIFFENDRING)
            .findFirst()
            .flatMap(InntektsmeldingValidererUtil::valideringTariffendring);
        if (feilmeldingTariffendring.isPresent()) {
            return feilmeldingTariffendring;
        }

        if (oppgitteEndringsårsaker.stream().anyMatch(årsak -> kreverFomDato(årsak.årsak()) && årsak.fom() == null)) {
            LOG.info("Endringsårsak mangler fra dato");
            return Optional.of(EksponertFeilmelding.ÅRSAK_KREVER_FRA_DATO);
        }

        var varigLønnsendringFraDato = oppgitteEndringsårsaker.stream()
            .filter(årsak -> årsak.årsak() == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.VARIG_LØNNSENDRING)
            .findFirst()
            .map(InntektsmeldingRequest.Endringsårsaker::fom);

        if (varigLønnsendringFraDato.isPresent() && varigLønnsendringFraDato.get().isAfter(startdato)) {
            LOG.info("Endringsårsak varig lønnsendring har ugyldig dato. Fra dato {} må være før fraværsdato {}",
                varigLønnsendringFraDato.get(),
                startdato);
            return Optional.of(EksponertFeilmelding.FRA_DATO_FØR_STARTDATO);
        }

        var årsakerSomKreverFomOgTomDato = oppgitteEndringsårsaker.stream()
            .filter(årsak -> kreverFomOgTomDato(årsak.årsak()))
            .toList();

        if (!årsakerSomKreverFomOgTomDato.isEmpty()) {
            if (årsakerSomKreverFomOgTomDato.stream().anyMatch(årsak -> årsak.fom() == null || årsak.tom() == null)) {
                LOG.info("Endringsårsak mangler fra eller til dato");
                return Optional.of(EksponertFeilmelding.ÅRSAK_KREVER_FRA_OG_TIL_DATO);
            }

            if (årsakerSomKreverFomOgTomDato.stream().anyMatch( årsak -> fraDatoEtterTom(årsak.fom(), årsak.tom()))) {
                LOG.info("Endringsårsak har ugyldig periode. Fra dato er etter til dato for en eller flere endringsårsaker");
                return Optional.of(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
            }

            if (finnesOverlapp(årsakerSomKreverFomOgTomDato,
                InntektsmeldingRequest.Endringsårsaker::fom,
                InntektsmeldingRequest.Endringsårsaker::tom)) {
                LOG.info("Endringsårsak har overlappende perioder");
                return Optional.of(EksponertFeilmelding.OVERLAPP_I_PERIODER);
            }
        }
        return Optional.empty();
    }

    private static Optional<EksponertFeilmelding> valideringTariffendring(InntektsmeldingRequest.Endringsårsaker endringsårsak) {
        if (endringsårsak != null) {
            if (endringsårsak.fom() == null || endringsårsak.bleKjentFom() == null) {
                LOG.info("Endringsårsak tariffendring mangler fra dato eller ble kjent fra dato");
                return Optional.of(EksponertFeilmelding.KREVER_FRA_OG_BLE_KJENT_DATO);
            }
            if (endringsårsak.bleKjentFom().isBefore(endringsårsak.fom())) {
                LOG.info("Endringsårsak tariffendring har ugyldig dato. Ble kjent fra dato {} er før fra dato {}",
                    endringsårsak.bleKjentFom(),
                    endringsårsak.fom());
                return Optional.of(EksponertFeilmelding.KREVER_FRA_OG_BLE_KJENT_DATO);
            }
        }
        return Optional.empty();
    }

    private static boolean fraDatoEtterTom(LocalDate fom, LocalDate tom) {
        if (fom == null || tom == null) {
            return false;
        }
        return fom.isAfter(tom);
    }

    /**
     * Generisk metode for å sjekke om perioder overlapper i en liste.
     * Bruker LocalDateInterval fra tidsserie-biblioteket for å sjekke overlapp.
     * Fungerer med alle typer som har datoperiode (fra og til dato).
     *
     * @param periods           listen av perioder som skal valideres
     * @param fromDateExtractor funksjon for å hente 'fra'-dato fra en periode
     * @param toDateExtractor   funksjon for å hente 'til'-dato fra en periode
     * @param <T>               typen periodeobjekt
     * @return true hvis perioder overlapper, false hvis ingen overlapp finnes
     */
    private static <T> boolean finnesOverlapp(List<T> periods,
                                              Function<T, LocalDate> fromDateExtractor,
                                              Function<T, LocalDate> toDateExtractor) {
        if (periods == null || periods.size() < 2) {
            return false;
        }

        // Konverter alle perioder til LocalDateInterval objekter
        var intervals = periods.stream()
            .map(p -> new LocalDateInterval(fromDateExtractor.apply(p), toDateExtractor.apply(p)))
            .toList();

        // Sjekk om noen interval overlapper med en annen
        return intervals.stream()
            .anyMatch(interval -> intervals.stream()
                .filter(other -> !interval.equals(other))
                .anyMatch(interval::overlaps));
    }

    private static boolean kreverFomDato(InntektsmeldingRequest.Endringsårsaker.Endringsårsak årsak) {
        return årsak == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.NY_STILLING
            || årsak == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.NY_STILLINGSPROSENT
            || årsak == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.VARIG_LØNNSENDRING;
    }

    private static boolean kreverFomOgTomDato(InntektsmeldingRequest.Endringsårsaker.Endringsårsak årsak) {
        return årsak == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE
            || årsak == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.PERMISJON
            || årsak == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.PERMITTERING
            || årsak == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.SYKEFRAVÆR;
    }

    private static boolean skalÅrsakVæreUnik(InntektsmeldingRequest.Endringsårsaker.Endringsårsak årsak) {
        return !(årsak == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE
            || årsak == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.PERMISJON
            || årsak == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.PERMITTERING
            || årsak == InntektsmeldingRequest.Endringsårsaker.Endringsårsak.SYKEFRAVÆR);
    }
}
