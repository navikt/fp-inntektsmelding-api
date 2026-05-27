# fp-inntektsmelding-api

External-facing REST API for submitting and retrieving income declarations
(inntektsmeldinger) for parental benefits (foreldrepenger) and pregnancy benefits
(svangerskapspenger). Consumed by payroll and HR systems (LPS – Lønns- og
personalsystemer) via Maskinporten system tokens.

## Context (read first)

- **fp-context** (https://github.com/navikt/fp-context) — team-wide domain,
  architecture, conventions, workflow. Treat as source of truth.
- **Copilot Space**: navikt / TeamForeldrepenger — attaches fp-context + key repos.
- Defer to fp-context for: domain/Folketrygdloven kap. 14, backend stack,
  Java code style, testing conventions, workflow/PR rules, CI/CD, dependency mgmt.

## Purpose

NAV sends a *forespørsel* (request) to an employer when an employee applies for
parental/pregnancy benefits. This API allows LPS systems to:

1. **Retrieve forespørsler** — fetch open and completed requests for a given organisation.
2. **Submit inntektsmeldinger** — send income declarations in response to a forespørsel.
3. **Retrieve inntektsmeldinger** — look up previously submitted declarations.

The API is the external gateway; it delegates storage and business logic to
**fp-inntektsmelding** (internal backend).

## Role in the value chain

| Upstream | fp-inntektsmelding-api | Downstream |
|---|---|---|
| LPS systems (external consumers) | Authentication, authorization, validation | fp-inntektsmelding (internal processing) |
| Maskinporten (token issuance) | Altinn authorization (PDP) | fp-sak (case processing) |

## Architecture

```
LPS → Maskinporten token → fp-inntektsmelding-api → fp-inntektsmelding → fp-sak
```

### Deviations from standard backend stack

This app diverges from the fp-context backend stack in these ways:

| Concern | Standard (fp-context) | This app |
|---|---|---|
| Database | Hibernate/JPA + Flyway | **None** — stateless, delegates to fp-inntektsmelding |
| Auth | Azure AD / TokenX + ABAC | **Maskinporten** system tokens + **Altinn 3 PDP** |
| Service-to-service | Azure AD CC | Azure AD CC (to fp-inntektsmelding) |
| API surface | Internal saksbehandler UI | **External** — consumed by third-party LPS |

### Package layout

| Package | Purpose |
|---|---|
| `tjenester.eksterne` | REST endpoints (ForespørselRest, InntektsmeldingRest) |
| `integrasjoner` | Client for fp-inntektsmelding backend (FpinntektsmeldingKlient) |
| `server` | Jetty bootstrap, API config |
| `server.auth` | Authentication filter, Altinn token exchange, PDP authorization |
| `forespørsel` | DTOs for forespørsel domain (note: uses Norwegian `ø` in package name) |
| `inntektsmelding` | DTOs and mapper for inntektsmelding domain |
| `typer` | Shared types and kodeverk mappings |

### Authentication & authorization flow

1. LPS obtains a Maskinporten system token with scope `nav:foreldrepenger/inntektsmeldingapi.write`.
2. `AutentiseringFilter` validates the token and extracts org number + system ID.
3. `TilgangTjeneste` calls Altinn PDP to verify the system has rights for the
   Altinn resource on behalf of the organisation.
4. Request proceeds to the REST endpoint.

## Repo-specific coding guidance

- **No JPA/Hibernate** — this is a stateless proxy app, no entity classes.
- **External API contract stability** — breaking changes to the REST API require
  versioning. The OpenAPI spec is the contract with external LPS consumers.
- **Validation** — org numbers follow the Norwegian format (9 digits, MOD11 check).
  Forespørsel IDs are UUIDs. Use Jakarta Bean Validation annotations.
- **Maskinporten scope**: `nav:foreldrepenger/inntektsmeldingapi.write`.
- Package names use Norwegian characters (`forespørsel` with `ø`) — this is
  intentional and consistent with the rest of the team's codebase.

## Testing

```bash
mvn test        # run all unit tests
```

- Unit tests use JUnit 6, AssertJ, and Mockito (constructor injection).
- No database — no testcontainers or Flyway in this repo.
- Mocking: Mockito mocks for `FpinntektsmeldingKlient`, `PdpKlient`, and auth
  components. No WireMock needed since there's no HTTP-level integration test.
- Integration tests run via [fp-autotest](https://github.com/navikt/fp-autotest)
  (`verdikjede` suite) against the full deployed stack.
