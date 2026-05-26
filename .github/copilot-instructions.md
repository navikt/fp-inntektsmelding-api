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

### Tech stack

- **Java 21**, Maven, Jetty (embedded), Jersey (JAX-RS)
- **CDI** (Weld) for dependency injection
- **Maskinporten** system tokens for authentication
- **Altinn 3 PDP** for authorization (verifies org has rights to the resource)
- **Azure AD CC** for service-to-service calls to fp-inntektsmelding
- OpenAPI/Swagger for API documentation

### Package layout

| Package | Purpose |
|---|---|
| `tjenester.eksterne` | REST endpoints (ForespørselRest, InntektsmeldingRest) |
| `integrasjoner` | Client for fp-inntektsmelding backend (FpinntektsmeldingKlient) |
| `server` | Jetty bootstrap, API config |
| `server.auth` | Authentication filter, Altinn token exchange, PDP authorization |
| `forespørsel` | DTOs for forespørsel domain |
| `inntektsmelding` | DTOs and mapper for inntektsmelding domain |
| `typer` | Shared types and kodeverk mappings |

### Authentication & authorization flow

1. LPS obtains a Maskinporten system token with the appropriate scope.
2. `AutentiseringFilter` validates the token and extracts org number + system ID.
3. `TilgangTjeneste` calls Altinn PDP to verify the system has rights for the
   resource on behalf of the organisation.
4. Request proceeds to the REST endpoint.

## Testing

- Unit tests use JUnit 5 and Mockito.
- No database in this application — it is stateless and delegates to fp-inntektsmelding.
- Integration tests against fp-inntektsmelding run via fp-autotest (verdikjede tests).
