package no.nav.foreldrepenger.inntektsmelding.api.tjenester.eksterne;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.api.typer.ForespørselStatus;

import no.nav.foreldrepenger.inntektsmelding.api.typer.OrganisasjonsnummerDto;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.foreldrepenger.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

class InntektsmeldingValidererUtilTest {

    private static final UUID DEFAULT_UUID = UUID.randomUUID();
    private static final String DEFAULT_FNR = "12345678901";
    private static final LocalDate STARTDATO = LocalDate.of(2025, 6, 1);
    private static final BigDecimal DEFAULT_BELØP = new BigDecimal("25000.00");


    // =====================================================================
    // validerInntektsmeldingMotForespørsel
    // =====================================================================

    @Test
    void skal_avvise_mismatch_ytelsetype() {
        var forespørsel = lagForespørsel(ForespørselStatus.UNDER_BEHANDLING, STARTDATO, YtelseTypeDto.SVANGERSKAPSPENGER);
        var result = InntektsmeldingValidererUtil.validerInntektsmeldingMotForespørsel(lagDefaultRequest(), forespørsel);
        assertThat(result).hasValue(EksponertFeilmelding.MISMATCH_YTELSE);
    }

    @Test
    void skal_godkjenne_gyldig_forespørsel() {
        var result = InntektsmeldingValidererUtil.validerInntektsmeldingMotForespørsel(lagDefaultRequest(), lagDefaultForespørsel());
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_utgått_forespørsel() {
        var forespørsel = lagForespørsel(ForespørselStatus.UTGÅTT, STARTDATO, YtelseTypeDto.FORELDREPENGER);
        var result = InntektsmeldingValidererUtil.validerInntektsmeldingMotForespørsel(lagDefaultRequest(), forespørsel);
        assertThat(result).hasValue(EksponertFeilmelding.UGYLDIG_FORESPØRSEL);
    }

    @Test
    void skal_avvise_mismatch_startdato() {
        var forespørsel = lagForespørsel(ForespørselStatus.UNDER_BEHANDLING, STARTDATO.plusDays(5), YtelseTypeDto.FORELDREPENGER);
        var result = InntektsmeldingValidererUtil.validerInntektsmeldingMotForespørsel(lagDefaultRequest(), forespørsel);
        assertThat(result).hasValue(EksponertFeilmelding.MISMATCH_FØRSTE_UTTAKSDATO);
    }

    @Test
    void skal_godkjenne_svangerskapspenger_matcher() {
        var request = lagRequest(InntektsmeldingRequest.YtelseType.SVANGERSKAPSPENGER,
            List.of(new InntektsmeldingRequest.Refusjon(STARTDATO, DEFAULT_BELØP)),
            Collections.emptyList(), Collections.emptyList());
        var forespørsel = lagForespørsel(ForespørselStatus.UNDER_BEHANDLING, STARTDATO, YtelseTypeDto.SVANGERSKAPSPENGER);
        var result = InntektsmeldingValidererUtil.validerInntektsmeldingMotForespørsel(request, forespørsel);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_ferdig_forespørsel() {
        var forespørsel = lagForespørsel(ForespørselStatus.FERDIG, STARTDATO, YtelseTypeDto.FORELDREPENGER);
        var result = InntektsmeldingValidererUtil.validerInntektsmeldingMotForespørsel(lagDefaultRequest(), forespørsel);
        assertThat(result).isEmpty();
    }

    // =====================================================================
    // validerRefusjon
    // =====================================================================

    @Test
    void skal_avvise_refusjon_uten_startdato_i_listen() {
        var refusjon = List.of(new InntektsmeldingRequest.Refusjon(STARTDATO.plusDays(1), DEFAULT_BELØP));
        var result = InntektsmeldingValidererUtil.validerRefusjon(refusjon, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.UGYLDIG_FRA_DATO_LISTE);
    }

    @Test
    void skal_godkjenne_null_refusjon() {
        var result = InntektsmeldingValidererUtil.validerRefusjon(null, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_enkel_refusjon_med_startdato() {
        var refusjon = List.of(new InntektsmeldingRequest.Refusjon(STARTDATO, DEFAULT_BELØP));
        var result = InntektsmeldingValidererUtil.validerRefusjon(refusjon, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_sammenhengende_refusjoner() {
        var refusjon = List.of(
            new InntektsmeldingRequest.Refusjon(STARTDATO, DEFAULT_BELØP),
            new InntektsmeldingRequest.Refusjon(STARTDATO.plusDays(1), new BigDecimal("20000.00")),
            new InntektsmeldingRequest.Refusjon(STARTDATO.plusDays(2), new BigDecimal("15000.00"))
        );
        var result = InntektsmeldingValidererUtil.validerRefusjon(refusjon, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_duplikat_fom_dato() {
        var refusjon = List.of(
            new InntektsmeldingRequest.Refusjon(STARTDATO, DEFAULT_BELØP),
            new InntektsmeldingRequest.Refusjon(STARTDATO, new BigDecimal("20000.00"))
        );
        var result = InntektsmeldingValidererUtil.validerRefusjon(refusjon, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.LIK_FOM_REFUSJON);
    }

    @Test
    @Disabled
    void skal_avvise_ikke_sammenhengende_refusjoner() {
        var refusjon = List.of(
            new InntektsmeldingRequest.Refusjon(STARTDATO, DEFAULT_BELØP),
            new InntektsmeldingRequest.Refusjon(STARTDATO.plusDays(3), new BigDecimal("20000.00"))
        );
        var result = InntektsmeldingValidererUtil.validerRefusjon(refusjon, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.UGYLDIG_FRA_DATO_LISTE);
    }

    @Test
    void skal_godkjenne_usorterte_sammenhengende_refusjoner() {
        var refusjon = List.of(
            new InntektsmeldingRequest.Refusjon(STARTDATO.plusDays(2), new BigDecimal("15000.00")),
            new InntektsmeldingRequest.Refusjon(STARTDATO, DEFAULT_BELØP),
            new InntektsmeldingRequest.Refusjon(STARTDATO.plusDays(1), new BigDecimal("20000.00"))
        );
        var result = InntektsmeldingValidererUtil.validerRefusjon(refusjon, STARTDATO);
        assertThat(result).isEmpty();
    }

    // =====================================================================
    // validerNaturalytelse
    // =====================================================================

    @Test
    void skal_godkjenne_null_naturalytelse() {
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(null);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_tom_liste() {
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_enkel_periode() {
        var perioder = List.of(lagNaturalytelse(STARTDATO, STARTDATO.plusDays(10)));
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(perioder);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_ikke_overlappende_perioder() {
        var perioder = List.of(
            lagNaturalytelse(STARTDATO, STARTDATO.plusDays(10)),
            lagNaturalytelse(STARTDATO.plusDays(12), STARTDATO.plusDays(20))
        );
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(perioder);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_overlappende_perioder() {
        var perioder = List.of(
            lagNaturalytelse(STARTDATO, STARTDATO.plusDays(10)),
            lagNaturalytelse(STARTDATO.plusDays(5), STARTDATO.plusDays(20))
        );
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(perioder);
        assertThat(result).hasValue(EksponertFeilmelding.OVERLAPP_I_PERIODER);
    }


    @Test
    void skal_avvise_fom_etter_tom() {
        var perioder = List.of(lagNaturalytelse(STARTDATO.plusDays(10), STARTDATO));
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(perioder);
        assertThat(result).hasValue(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
    }

    @Test
    void skal_godkjenne_periode_med_kun_fom() {
        var perioder = List.of(lagNaturalytelse(STARTDATO, null));
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(perioder);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_sammenhengende_perioder() {
        var perioder = List.of(
            lagNaturalytelse(STARTDATO, STARTDATO.plusDays(10)),
            lagNaturalytelse(STARTDATO.plusDays(11), STARTDATO.plusDays(20))
        );
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(perioder);
        assertThat(result).isEmpty();
    }

    private InntektsmeldingRequest.BortfaltNaturalytelse lagNaturalytelse(LocalDate fom, LocalDate tom) {
        return new InntektsmeldingRequest.BortfaltNaturalytelse(fom, tom,
            InntektsmeldingRequest.BortfaltNaturalytelse.Naturalytelsetype.ELEKTRISK_KOMMUNIKASJON, DEFAULT_BELØP);
    }

    // =====================================================================
    // validerEndringsårsaker
    // =====================================================================

    @Test
    void skal_godkjenne_null_endringsårsaker() {
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(null, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_tom_endringsårsak_liste() {
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(Collections.emptyList(), STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_duplikate_unike_årsaker() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.NY_STILLING, STARTDATO.minusDays(5), null, null),
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.NY_STILLING, STARTDATO.minusDays(3), null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.DUPLIKATER_IKKE_TILATT);
    }

    @Test
    void skal_godkjenne_duplikate_ikke_unike_årsaker() {
        // FERIE, PERMISJON, PERMITTERING, SYKEFRAVÆR er lov å ha flere av
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE, STARTDATO, STARTDATO.plusDays(5), null),
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE, STARTDATO.plusDays(10), STARTDATO.plusDays(15), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_duplikate_permisjon_årsaker() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.PERMISJON, STARTDATO, STARTDATO.plusDays(5), null),
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.PERMISJON, STARTDATO.plusDays(10), STARTDATO.plusDays(15), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_duplikat_bonus() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.BONUS, null, null, null),
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.BONUS, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.DUPLIKATER_IKKE_TILATT);
    }

    @Test
    void skal_avvise_tariffendring_uten_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.TARIFFENDRING, null, null, STARTDATO)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.KREVER_FRA_OG_BLE_KJENT_DATO);
    }

    @Test
    void skal_avvise_tariffendring_uten_ble_kjent_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.TARIFFENDRING, STARTDATO, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.KREVER_FRA_OG_BLE_KJENT_DATO);
    }

    @Test
    void skal_avvise_tariffendring_ble_kjent_før_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.TARIFFENDRING, STARTDATO, null, STARTDATO.minusDays(1))
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.KREVER_FRA_OG_BLE_KJENT_DATO);
    }

    @Test
    void skal_godkjenne_gyldig_tariffendring() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.TARIFFENDRING, STARTDATO, null, STARTDATO.plusDays(5))
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_tariffendring_ble_kjent_lik_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.TARIFFENDRING, STARTDATO, null, STARTDATO)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }


    @Test
    void skal_avvise_ny_stilling_uten_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.NY_STILLING, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.ÅRSAK_KREVER_FRA_DATO);
    }

    @Test
    void skal_avvise_ny_stillingsprosent_uten_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.NY_STILLINGSPROSENT, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.ÅRSAK_KREVER_FRA_DATO);
    }

    @Test
    void skal_avvise_varig_lønnsendring_uten_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.VARIG_LØNNSENDRING, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.ÅRSAK_KREVER_FRA_DATO);
    }

    @Test
    void skal_godkjenne_ny_stilling_med_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.NY_STILLING, STARTDATO.minusDays(10), null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_varig_lønnsendring_fom_etter_startdato() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.VARIG_LØNNSENDRING, STARTDATO.plusDays(1), null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.FRA_DATO_FØR_STARTDATO);
    }

    @Test
    void skal_godkjenne_varig_lønnsendring_fom_lik_startdato() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.VARIG_LØNNSENDRING, STARTDATO, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_varig_lønnsendring_fom_før_startdato() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.VARIG_LØNNSENDRING, STARTDATO.minusDays(10), null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_ferie_uten_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE, null, STARTDATO.plusDays(5), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.ÅRSAK_KREVER_FRA_OG_TIL_DATO);
    }

    @Test
    void skal_avvise_ferie_uten_tom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE, STARTDATO, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.ÅRSAK_KREVER_FRA_OG_TIL_DATO);
    }

    @Test
    void skal_avvise_permittering_uten_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.PERMITTERING, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.ÅRSAK_KREVER_FRA_OG_TIL_DATO);
    }

    @Test
    void skal_avvise_sykefravær_uten_tom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.SYKEFRAVÆR, STARTDATO, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.ÅRSAK_KREVER_FRA_OG_TIL_DATO);
    }

    @Test
    void skal_godkjenne_ferie_med_fom_og_tom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE, STARTDATO, STARTDATO.plusDays(5), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_permisjon_med_fom_og_tom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.PERMISJON, STARTDATO, STARTDATO.plusDays(10), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_fom_etter_tom_for_endringsårsaker() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE, STARTDATO.plusDays(10), STARTDATO, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
    }

    @Test
    void skal_godkjenne_fom_lik_tom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE, STARTDATO, STARTDATO, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_overlappende_perioder_for_endringsårsaker() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE, STARTDATO, STARTDATO.plusDays(10), null),
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.PERMISJON, STARTDATO.plusDays(5), STARTDATO.plusDays(15), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.OVERLAPP_I_PERIODER);
    }

    @Test
    void skal_godkjenne_ikke_overlappende_perioder_for_endringsårsaker() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE, STARTDATO, STARTDATO.plusDays(5), null),
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.PERMISJON, STARTDATO.plusDays(7), STARTDATO.plusDays(15), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_bonus_uten_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.BONUS, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_nyansatt_uten_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.NYANSATT, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_ferietrekk_uten_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    // =====================================================================
    // validerInntektsmelding (orchestration)
    // =====================================================================

    @Test
    void skal_godkjenne_mangelfull_rapportering_uten_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.MANGELFULL_RAPPORTERING_AORDNING, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_inntekt_ikke_rapportert_uten_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_flere_ulike_årsaker_med_gyldige_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.NY_STILLING, STARTDATO.minusDays(5), null, null),
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.BONUS, null, null, null),
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE, STARTDATO, STARTDATO.plusDays(5), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_ikke_godkjenne_flere_ulike_årsaker_hvor_en_har_ugyldig_dato() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.NY_STILLING, STARTDATO.minusDays(5), null, null),
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.BONUS, null, null, null),
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE, STARTDATO.minusDays(5), STARTDATO.plusDays(5), null),
            lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak.FERIE, STARTDATO.minusDays(4), STARTDATO.plusDays(5), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.OVERLAPP_I_PERIODER);
    }

    private InntektsmeldingRequest.Endringsårsaker lagEndringsårsak(InntektsmeldingRequest.Endringsårsaker.Endringsårsak årsak,
                                                                    LocalDate fom, LocalDate tom, LocalDate bleKjentFom) {
        return new InntektsmeldingRequest.Endringsårsaker(årsak, fom, tom, bleKjentFom);
    }

    @Test
    void skal_godkjenne_gyldig_inntektsmelding() {
        var result = InntektsmeldingValidererUtil.validerInntektsmelding(lagDefaultRequest(), lagDefaultForespørsel());
        assertThat(result).isEmpty();
    }


    @Test
    void skal_returnere_feil_fra_refusjon_validering() {
        var request = lagRequest(InntektsmeldingRequest.YtelseType.FORELDREPENGER,
            List.of(new InntektsmeldingRequest.Refusjon(STARTDATO.plusDays(5), DEFAULT_BELØP)),
            Collections.emptyList(), Collections.emptyList());
        var result = InntektsmeldingValidererUtil.validerInntektsmelding(request, lagDefaultForespørsel());
        assertThat(result).hasValue(EksponertFeilmelding.UGYLDIG_FRA_DATO_LISTE);
    }

    @Test
    void skal_returnere_feil_fra_naturalytelse_validering() {
        var request = lagRequest(InntektsmeldingRequest.YtelseType.FORELDREPENGER,
            List.of(new InntektsmeldingRequest.Refusjon(STARTDATO, DEFAULT_BELØP)),
            List.of(new InntektsmeldingRequest.BortfaltNaturalytelse(STARTDATO.plusDays(10), STARTDATO,
                InntektsmeldingRequest.BortfaltNaturalytelse.Naturalytelsetype.BIL, DEFAULT_BELØP)),
            Collections.emptyList());
        var result = InntektsmeldingValidererUtil.validerInntektsmelding(request, lagDefaultForespørsel());
        assertThat(result).hasValue(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
    }

    @Test
    void skal_returnere_feil_fra_endringsårsak_validering() {
        var request = lagRequest(InntektsmeldingRequest.YtelseType.FORELDREPENGER,
            List.of(new InntektsmeldingRequest.Refusjon(STARTDATO, DEFAULT_BELØP)),
            Collections.emptyList(),
            List.of(new InntektsmeldingRequest.Endringsårsaker(
                InntektsmeldingRequest.Endringsårsaker.Endringsårsak.NY_STILLING, null, null, null)));
        var result = InntektsmeldingValidererUtil.validerInntektsmelding(request, lagDefaultForespørsel());
        assertThat(result).hasValue(EksponertFeilmelding.ÅRSAK_KREVER_FRA_DATO);
    }

    // --- Hjelpemetoder for testdata ---
    private static InntektsmeldingRequest lagRequest(InntektsmeldingRequest.YtelseType ytelse,
                                                     List<InntektsmeldingRequest.Refusjon> refusjon,
                                                     List<InntektsmeldingRequest.BortfaltNaturalytelse> naturalytelser,
                                                     List<InntektsmeldingRequest.Endringsårsaker> endringsårsaker) {
        return new InntektsmeldingRequest(
            DEFAULT_UUID, DEFAULT_FNR, InntektsmeldingValidererUtilTest.STARTDATO, ytelse,
            new InntektsmeldingRequest.Kontaktperson("Test Person", "99887766"),
            DEFAULT_BELØP, refusjon, naturalytelser, endringsårsaker,
            new InntektsmeldingRequest.AvsenderSystem("TestSystem", "1.0"));
    }

    private static InntektsmeldingRequest lagDefaultRequest() {
        return lagRequest(InntektsmeldingRequest.YtelseType.FORELDREPENGER,
            List.of(new InntektsmeldingRequest.Refusjon(STARTDATO, DEFAULT_BELØP)),
            Collections.emptyList(), Collections.emptyList());
    }

    private static Forespørsel lagForespørsel(ForespørselStatus status, LocalDate førsteUttaksdato, YtelseTypeDto ytelseType) {
        return new Forespørsel(DEFAULT_UUID, new OrganisasjonsnummerDto("999999999"), DEFAULT_FNR, førsteUttaksdato,
            LocalDate.of(2025, 5, 1), status, ytelseType, LocalDateTime.now());
    }

    private static Forespørsel lagDefaultForespørsel() {
        return lagForespørsel(ForespørselStatus.UNDER_BEHANDLING, STARTDATO, YtelseTypeDto.FORELDREPENGER);
    }
}
