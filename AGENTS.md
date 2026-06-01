# fpinntektsmelding-api — Agent Instructions

For team-wide context, see [fp-context](https://github.com/navikt/fp-context)
and the **TeamForeldrepenger** Copilot Space.

## Integration testing

Lives in [fp-autotest](https://github.com/navikt/fp-autotest).

fp-inntektsmelding-api is part of the deployed stack used by the `verdikjede`
and `fpsak` suites. Inntektsmeldinger are submitted through this API as part
of end-to-end tests for foreldrepenger and svangerskapspenger flows.

```bash
cd ~/git/fp-autotest
mvn test -P verdikjede               # end-to-end (uses fp-inntektsmelding-api)
mvn test -P fpsak -Dtest=Fodsel      # single fpsak class (submits IM via API)
```

## Local build for autotest

| Item | Value |
|---|---|
| Docker image env var | `FPINNTEKTSMELDINGAPI_IMAGE` |
| Compose service | `fpinntektsmeldingapi` |
| Container name | `fpinntektsmelding-api` |

Build and deploy locally:

```bash
mvn package -DskipTests
docker build -t fp-inntektsmelding-api .
# Update FPINNTEKTSMELDINGAPI_IMAGE in fp-autotest pipeline/.env to local tag
```
