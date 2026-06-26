# fp-inntektsmelding-api

External REST API that lets payroll/HR systems (LPS – Lønns- og personalsystemer, i.e. employer ERP systems) 
retrieve requests (forespørsler) and submit/retrieve income declarations and reimbursment claims (inntektsmeldinger)
for foreldrepenger and svangerskapspenger. The **only externally consumed app** in the
ecosystem. Stateless gateway — all storage and business logic live in `fp-inntektsmelding`.

## Shared context

- Source of truth for shared domain, architecture, and conventions: `navikt/fp-context`
- Copilot Space: `navikt/TeamForeldrepenger`
- Defer to fp-context for domain (Folketrygdloven kap. 14), Java code style, testing, workflow/PR, CI/CD, and dependency management.

## Repo-specific context

| Topic      | Details |
|------------|---------|
| Role       | External gateway for LPS → forespørsel-oppslag + inntektsmelding innsending/oppslag |
| Consumers  | Third-party LPS/ERP systems (external), authenticated via Maskinporten |
| Downstream | `fp-inntektsmelding` (processing) → `fp-sak` (case processing) |
| Tech stack | Jakarta REST (Jersey) on Jetty + Weld CDI; **no** `fp-prosesstask` |
| Data       | **None** — stateless proxy; no JPA/Hibernate/Flyway |
| Kontrakt   | DTOs from `inntektsmelding-kontrakt` (released by `fp-inntektsmelding`) |

## Deviations from the standard fp backend (read carefully)

| Concern | Standard (fp-context) | This app |
|---|---|---|
| API surface | Internal saksbehandler/system | **External** — third-party LPS; OpenAPI is the contract |
| Inbound auth | Azure AD / TokenX + ABAC | **Maskinporten** system token |
| Authorization | Nav ABAC/XACML | **Altinn 3** token exchange + PDP (authorize on behalf of org) |
| Service-to-service | — | Azure AD (outbound to `fp-inntektsmelding`) |
| Persistence | PostgreSQL/Oracle + Flyway | none |

## Authentication & authorization flow

`LPS → Maskinporten token → fp-inntektsmelding-api → (Azure AD) → fp-inntektsmelding`

1. LPS calls with a Maskinporten system token for the exposed scope `nav:inntektsmelding/foreldrepenger`.
2. `AutentiseringFilter` (JAX-RS `@Provider`) → `AuthTjeneste.validerOgSettKontekst` validates the token and required scope.
3. `Tilgang` / `TilgangTjeneste.sjekkAtSystemHarTilgangTilOrganisasjon` authorizes the caller against the requested organisation via Altinn: `AltinnTokenExchangeKlient` (exchanges for an Altinn token using the consumed scope `altinn:authorization/authorize`) + `PdpKlient` (Altinn 3 PDP XACML authorize).
4. Endpoint delegates to `FpinntektsmeldingTjeneste` → `FpinntektsmeldingKlient` (Azure AD) against `fp-inntektsmelding`.

Maskinporten scopes (`exposes`/`consumes`) and the Altinn external host live in `.deploy/naiserator.yaml`.

## Package layout (root `no.nav.foreldrepenger.inntektsmelding.api`)

| Package | Purpose |
|---|---|
| `tjenester.eksterne` | External REST endpoints: `ForespørselRest`, `InntektsmeldingRest` |
| `integrasjoner` | Backend client: `FpinntektsmeldingTjeneste`, `FpinntektsmeldingKlient` |
| `server.auth` | `AutentiseringFilter`, `AuthTjeneste`, `Tilgang`/`TilgangTjeneste`; `altinn/` token exchange, `altinnPdp/` PDP client |
| `server.app.api` | `ApiConfig` (`@ApplicationPath("/v1")`), OpenAPI |
| `server.app.internal` | health + Prometheus endpoints |
| `server.exceptions` | `InntektsmeldingAPIException`, `ErrorResponse`, `EksponertFeilmelding` |
| `forespørsel` / `inntektsmelding` | DTOs + mappers (note Norwegian `ø` in package name — intentional) |
| `typer` | shared types / kodeverk mapping |

## Repo-specific coding guidance

- **Stateless** — no entity classes, no DB; everything proxies to `fp-inntektsmelding`.
- **External contract stability** — the OpenAPI spec under `/v1` is the contract with LPS; breaking changes require versioning.
- **Validation** — Jakarta Bean Validation on request DTOs; forespørsel/inntektsmelding IDs are UUIDs (regex-validated), orgnr is Norwegian 9-digit.
- **Error responses** — return `ErrorResponse` with an `EksponertFeilmelding` code; never leak internal details to external callers.
- Norwegian characters in package/identifier names are intentional and team-consistent.

## Testing & verification

- `mvn test` — JUnit + AssertJ + Mockito (constructor injection); versions inherited from `fp-parent-app`. No DB, no Testcontainers.
- Mock `FpinntektsmeldingKlient`/`FpinntektsmeldingTjeneste`, `PdpKlient`, and auth components.
- Integration impact: verify via `navikt/fp-autotest`, `verdikjede` suite, against the deployed stack.
