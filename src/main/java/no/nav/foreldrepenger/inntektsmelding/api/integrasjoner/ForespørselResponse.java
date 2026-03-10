package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import no.nav.foreldrepenger.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.api.typer.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

record ForespørselResponse(UUID forespørselUuid, OrganisasjonsnummerDto orgnummer, String fødselsnummer, LocalDate førsteUttaksdato,
                                  LocalDate skjæringstidspunkt, ForespørselStatus status, YtelseTypeDto ytelseType, LocalDateTime opprettetTid) {}
