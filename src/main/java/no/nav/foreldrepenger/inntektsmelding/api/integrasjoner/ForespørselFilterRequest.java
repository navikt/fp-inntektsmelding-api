package no.nav.foreldrepenger.inntektsmelding.api.integrasjoner;

import no.nav.foreldrepenger.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.api.typer.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.api.typer.YtelseTypeDto;

import java.time.LocalDate;

record ForespørselFilterRequest(OrganisasjonsnummerDto orgnr, FødselsnummerDto fnr,
                                          ForespørselStatus status, YtelseTypeDto ytelseType, LocalDate fom, LocalDate tom) {}
