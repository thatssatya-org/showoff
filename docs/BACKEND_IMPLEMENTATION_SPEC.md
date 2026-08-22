# Backend implementation specification

**Audience:** implementation LLMs and backend engineers  
**Status:** final build contract  
**Primary references:** `REQUIREMENTS.md`, `docs/VENDOR_CREDENTIALS.md`, `samsepiol-bom`, `samsepiol-library`, and `file-nexus`

This document is prescriptive. Do not replace its patterns with a “modern” alternative, invent a missing dependency coordinate, scrape a vendor, or silently weaken a security/availability rule. If a requirement conflicts with a library capability, stop at the library boundary and add/release the missing abstraction there first.

## 1. Scope and build outcome

Build `apps/api`, a Spring Boot API that supplies public, cached portfolio content and refreshes vendor snapshots out of band. It is a single-owner, self-hosted installation. It must work when zero, some, or all optional vendor profiles are configured.

The visitor request path reads only precomputed content/snapshots. It never calls GitHub, Spotify, Meta, Google, LinkedIn, SMTP, Temporal, or a homelab service synchronously.

### 1.1 Non-negotiable outcomes

- One API binary/container, MongoDB as all portfolio-owned persistence, and self-hosted Temporal for durable orchestration.
- Listmonk integration is through a private internal HTTP interface. Listmonk owns its mandated PostgreSQL store; the portfolio API does not use PostgreSQL, JPA, Flyway, Hibernate, Spring Data relational repositories, or SQL.
- All vendor adapters are official API adapters or manual content fallbacks. No screen scraping, browser automation, cookie import, or third-party feed component.
- Optional capability absent/disabled/unapproved/empty means **`204 No Content` and no response body**, never an exception or a fake empty object.
- A healthy stale snapshot remains renderable with `200 OK` and timestamp/source metadata when the upstream vendor is unavailable.
- Secrets are environment-injected, encrypted at rest where stored, redacted in logs, and absent from OpenAPI, tests, documentation examples, metrics, browser payloads, and error bodies.

## 2. Platform, modules, and dependency gate

### 2.1 Maven baseline

Use the BOM and library, not dependency versions copied from this file.

```text
BOM group/artifact:       com.samsepiol:bom
Library group:            com.samsepiol.library
Library module artifacts: library-core, library-application, repository-models,
                          mongo, http, temporal, cache-core/guava or redis,
                          lock, health
Reference implementation: thatssatya-org/file-nexus
```

The public repository audit on 2026-08-23 found BOM `0.0.5-BOM-SNAPSHOT` declaring Java 21 and Spring Boot 3.3.4, while the library root was `0.0.4-LIBRARY-SNAPSHOT`. These are **not** permission to hard-code snapshots, claim Java 25 support, or guess a Maven repository. Before implementation, the operator must provide a resolvable released version/repository pair. The API uses the Java/Spring level dictated by that released BOM. A Java 25/Spring upgrade belongs in a BOM release first.

### 2.2 Required project layout

```text
apps/api/
  pom.xml
  src/main/java/com/samsepiol/portfolio/
    PortfolioApiApplication.java
    api/                 # controllers + public DTOs only
    application/         # readable business services/use cases
    domain/              # enums, policies, capability contracts
    provider/            # Strategy interfaces, implementations, factory
    repository/          # Mongo document repositories/adapters + codecs
    temporal/            # workflow/activity interfaces and orchestration only
    configuration/       # explicit Spring configuration/properties
    security/            # OAuth state, crypto, request protection
  src/main/resources/
    application.yaml
    application-local.yaml
  src/test/java/...      # mirrors production packages
```

Do not create a generic `util`, `common`, `helper`, `manager`, `impl` dumping ground. The only acceptable `impl` package is a concrete implementation beside its narrow interface where the name cannot be expressed more clearly.

### 2.3 Dependency rules

- Import/parent the released Samsepiol BOM. Versions managed by it are omitted from application dependencies.
- Use the library `mongo` and `repository-models` module, including its `Entity` base type and codec registry. No second `MongoClient` configuration and no repository abstraction that bypasses library codecs.
- Use library `http` for outbound vendor/Listmonk calls. Do not introduce `RestTemplate`, another HTTP library, a raw `WebClient` wrapper, or a vendor SDK unless first added to the library.
- Use library cache/lock abstractions. Start with the library’s local cache option. Redis is not a default; introduce it only behind the library when scaling proves it necessary.
- Use library `temporal`. Temporal Java SDK is transitive/managed through it, not independently versioned.
- Use `library-application`/`health` only if their supplied defaults fit the desired security footprint; internal Actuator must remain private.
- If a dependency is absent from the BOM/library, open/change/release the platform repository first. Do not sidestep the rule to unblock this application.

## 3. Configuration and secrets

### 3.1 Environment-only binding

Bind environment variables through immutable, validated `@ConfigurationProperties` records/value objects. Do not read `System.getenv` outside configuration, use static secret fields, commit an `.env`, or add credentials to `application*.yaml`.

Required configuration groups:

```text
SPRING_DATA_MONGODB_*
PORTFOLIO_PUBLIC_BASE_URL
PORTFOLIO_CRYPTO_*
PORTFOLIO_OAUTH_STATE_SIGNING_KEY
PORTFOLIO_GITHUB_*
PORTFOLIO_SPOTIFY_*
PORTFOLIO_META_*
PORTFOLIO_YOUTUBE_*
PORTFOLIO_LINKEDIN_*
PORTFOLIO_LISTMONK_*
PORTFOLIO_SMTP_*
PORTFOLIO_TEMPORAL_*
PORTFOLIO_COLLECTOR_*
```

Only load/validate a provider’s required group when its operator profile enables that provider/capability. A deployment containing no Spotify credentials must start normally and its Spotify capability returns `204`.

### 3.2 Secret lifecycle

- Encrypt persisted OAuth refresh tokens using versioned AES-GCM envelope encryption. Persist ciphertext, key ID, nonce, algorithm/version, and rotation metadata—never plaintext.
- OAuth state uses a short-lived, signed, HttpOnly, Secure, SameSite state cookie and a server-side one-time correlation record. Validate exact redirect URI and state before token exchange.
- Create one HMAC secret per collector identity. Verify algorithm, timestamp window, nonce/replay record, body digest, and allow-listed schema before persistence.
- Produce only redacted configuration/error diagnostics. Tests assert that token-like text cannot enter logs or RFC 9457 problem detail.

## 4. Data model and MongoDB rules

### 4.1 Collections

| Collection | Minimum indexed paths | Write owner | Public read behaviour |
| --- | --- | --- | --- |
| `siteContent` | unique content key; visibility/order as queried | owner-content service | projection only |
| `projects` | unique slug; visibility; display order | project service | projection only |
| `providerProfiles` | unique provider type; enabled; capability | provider profile service | never public directly |
| `externalSnapshots` | unique `{capability, profileId}`; `validUntil`; `refreshedAt` | provider sync service | approved public projection only |
| `newsletterRequests` | unique idempotency key; short retention expiry | newsletter service | never public |
| `homelabSummaries` | collector ID + observed timestamp; publish timestamp | collector service | allow-listed projection only |
| `auditEvents` | actor + timestamp; event type + timestamp | management services | Tailnet/operator only |
| `replayNonces` | unique nonce; TTL expiry | collector/OAuth security service | never public |

Indexes are source code/migration-equivalent configuration and are tested. A query is not allowed until its exact compound index and projection are declared. Use TTL indexes for replay nonces and any temporary consent/idempotency record with retention requirements.

### 4.2 Immutable entity contract

Every Mongo entity follows the File Nexus pattern. Required fields use `@NonNull`; do not use `null` as an application state signal. Nullable persistence fields are allowed only when the domain explicitly permits absence and the public endpoint handles it deliberately.

```java
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Jacksonized
@SuperBuilder(toBuilder = true)
@Value
@AllArgsConstructor(onConstructor_ = {@BsonCreator})
public final class ProviderProfileEntity extends Entity {
    private static final String ID_PREFIX = "PP";

    @NonNull
    @BsonProperty("providerType")
    ProviderType providerType;

    @Override
    protected @NonNull String getIdPrefix() {
        return ID_PREFIX;
    }
}
```

Use `toBuilder()` to produce a new state for persistence. No mutable setters, mutable collections, mutable DTO fields, or mutation of a request/response after construction. Stored documents may change through explicit atomic update/replacement operations only.

### 4.3 Read/write discipline

- Request only fields needed by the caller with a BSON projection.
- Use exact filters, sorted/limited queries, cursor pagination where arrays can grow, ETags/conditional reads, and library codecs.
- Enforce unique indexes for idempotency and profile/capability identity; map duplicate-key outcomes to idempotent domain results, not a 500.
- Use an atomic Mongo transaction only when strictly required inside Mongo. Any transition that also controls a vendor/Listmonk/collector side effect is a Temporal workflow with idempotent activities and compensation/reconciliation.
- No `findAll`, no unbounded aggregation, no read-modify-write race, no unindexed regex search, no raw document dump to controller.

## 5. Domain contract and extension model

### 5.1 Enums

At minimum:

```text
ProviderType: GITHUB, SPOTIFY, INSTAGRAM, YOUTUBE, LINKEDIN, HOMELAB, LISTMONK
CapabilityType: GITHUB_ACTIVITY, GITHUB_REPOSITORIES, SPOTIFY_ON_REPEAT,
                INSTAGRAM_MEDIA, YOUTUBE_UPLOADS, LINKEDIN_SELECTED_POSTS,
                HOMELAB_SUMMARY
CapabilityState: ENABLED, DISABLED, AWAITING_AUTHORIZATION, HIDDEN, STALE, HEALTHY
ComponentType: ACTIVITY_TIMELINE, REPOSITORY_GRID, MUSIC_CARD, SOCIAL_GRID,
               HOMELAB_SUMMARY
```

Do not expose secret-bearing state, private vendor account IDs, or raw upstream error payloads through public enums/DTOs.

### 5.2 Strategy and factory pattern

Each provider/capability implementation has a narrow interface that returns its enum key. The factory receives the injected bean list once, validates duplicate/missing keys at startup, and creates an immutable `EnumMap`.

```java
public interface CapabilitySnapshotStrategy {
    @NonNull CapabilityType capabilityType();
    @NonNull CapabilitySnapshotRefreshResponse refresh(
            @NonNull CapabilitySnapshotRefreshRequest request);
}
```

The factory has no `if (provider == ...)` chain and no string map. Controllers and workflows call a generic capability service; they do not know concrete provider classes. A new vendor requires its enum, Strategy, configuration/profile validation, immutable mapper/entity, indexes, tests, and one registration—not a controller branch.

### 5.3 Capability API semantic contract

```text
GET /api/v1/capabilities
  200: list of public enabled capability descriptors only

GET /api/v1/capabilities/{capability}
  200: immutable typed public snapshot when configured and owner-approved
  204: profile missing, disabled, unauthorised, hidden, or no approved snapshot
  400: unknown/malformed capability path value
  429: public caller limited
  5xx: only a genuine API failure, RFC 9457 response, no upstream detail
```

`204` has no JSON body. The generated TypeScript client maps it to `null`; the frontend omits the card. A stale last-known-good snapshot is a `200`, explicitly labelled with `refreshedAt` and stale/source state. The descriptor list never announces an unavailable capability.

Provider-specific endpoints may exist as documented aliases but must delegate to the generic capability service and never have separate fetch/storage logic.

### 5.4 Public DTO contract

All public and internal transfer objects are immutable and use:

```java
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CapabilityDescriptorResponse { ... }
```

Controller input validates length, shape, enum, URL, and email constraints at the edge. Unknown JSON input fields are ignored only where the declared Jackson policy requires it; security-sensitive commands must additionally validate the accepted shape and reject policy-violating input. Serialize compact JSON, omit nulls, compress response bodies, and set `Cache-Control`/`ETag` where applicable.

## 6. Provider synchronization lifecycle

1. A schedule, protected operator action, or Temporal workflow asks the generic refresh service for one capability.
2. The factory resolves its Strategy. The Strategy delegates transport to the shared HTTP abstraction and maps only required upstream fields into an immutable service response.
3. The service applies approval/display policy, writes a new snapshot atomically, preserves the last known good snapshot on error, and records a redacted audit/sync status.
4. Public requests retrieve a projected snapshot only. They cannot force refresh.
5. A profile is not configured/approved: do not call upstream; return `204` through the public capability path.

Use vendor ETag/`If-None-Match` when available, explicit connect/read/write timeouts, bounded retry with jitter only for safe/idempotent operations, circuit breaking, and rate-limit-aware scheduling. Never retry token exchanges, newsletter subscription sends, or external mutations blindly.

## 7. Temporal boundaries

Use workflows for durable transactions that span Mongo and outside systems:

- OAuth connection, token persistence, and approval state transition.
- Token refresh/rotation/revocation with a compensation/reconciliation path.
- Newsletter subscription hand-off and confirmation-state reconciliation.
- Collector snapshot validation, durable persistence, delayed publication, and audit record.
- Any multi-document plus external observable state transition.

Workflow code is deterministic orchestration only: activity invocation, identifiers, retries/timeouts, compensation branching, and state hand-off. Activity code only bridges Temporal to a named Spring service. Business validation, provider mapping, persistence, and policy decisions belong in Spring services. No controller/business logic inside a workflow/activity; no sleeping, blocking I/O, random UUID generation, system time, database call, or HTTP call directly in workflow code.

## 8. Security and network contract

- Public API allows only public GET resources, newsletter POST, health liveness, and exact OAuth callback paths. Management/Actuator/collector routes are a distinct Tailnet/private path and security filter chain.
- TLS terminates at the trusted proxy. Require trusted proxy configuration; never trust arbitrary forwarded headers.
- Set strict CSP at the web tier; API applies `nosniff`, frame deny, referrer policy, HSTS only after domain/TLS validation, request body limits, and rate limiting.
- Use UUID/opaque identifiers as appropriate; never use a provider handle to authorise an operation.
- Allow-list outbound vendor base URLs. Reject arbitrary callback/fetch URLs. Encode all displayed upstream strings before rendering.
- Newsletter POST requires origin/same-site validation, honeypot, size/format checks, idempotency, and generic success wording. Do not log email addresses.
- Public health response is liveness only. Dependencies, versions, DB state, configuration, and stack traces are internal.

## 9. Test-driven development plan

Write the failing test before production implementation. One red-green-refactor slice at a time. Every issue/PR must name the tests added and show the final test command/output.

### 9.1 Test layers

| Layer | Tools/boundary | Required cases |
| --- | --- | --- |
| Unit | JUnit 5, AssertJ/Mockito only at interface boundaries | DTO immutability, capability factory duplicate/missing key failure, policy state mapping, snapshot mapper, validation, crypto envelope contract |
| Controller | `@WebMvcTest` or equivalent | `200` DTO/ETag, exact bodyless `204`, invalid input, 429/400 problem response, no secret/error leak |
| Repository | Mongo Testcontainers or isolated local Mongo | codec round-trip, `@BsonCreator` construction, index creation, projection shape, unique/idempotency/TTL behaviour |
| Provider adapter | mock shared HTTP abstraction | ETag 304, pagination/limit, malformed vendor JSON ignored safely, timeout/429/5xx last-good behaviour, no raw vendor payload propagation |
| Workflow | Temporal test environment through library | retries, idempotent activities, compensation/reconciliation, workflow contains no business-policy unit under test |
| Integration | Docker Compose/Testcontainers | Mongo + Temporal round trip, optional provider is 204, configured provider snapshot is 200, startup succeeds without optional credentials |
| Security | integration tests | callback state replay rejected, collector replay rejected, public routes cannot reach management, logs redacted, no outbound arbitrary host |
| Contract | OpenAPI generated client | frontend client treats `204` as null; OpenAPI compatibility snapshot is deliberate |

### 9.2 Minimum first vertical slices

1. `GET /healthz`: red test for a minimal liveness response; no dependency data.
2. `GET /api/v1/capabilities/{capability}` when profile absent: red controller/service test; implement bodyless 204.
3. Mongo `ProviderProfileEntity` codec/index/repository projection test before persistence implementation.
4. Generic Strategy factory test: duplicate enum key fails application startup; missing optional provider does not.
5. GitHub cached fixture Strategy test: provider response maps to snapshot, then public response returns a 200 ETag.
6. Provider error after prior snapshot test: returns stale public 200 without attempting visitor-path network I/O.
7. Newsletter idempotency/PII redaction test before Listmonk transport work.
8. Temporal provider-connection orchestration test before OAuth mutation flow.

No test relies on a live personal vendor credential. Use redacted JSON fixtures and mock transports. Tests that need infrastructure use local ephemeral/Testcontainers configuration, not the operator’s homelab.

## 10. Backend completion checklist

- [ ] BOM/library coordinates resolve from an operator-approved release repository; no invented version or dependency fallback.
- [ ] Every mutable application concern is represented by immutable Java objects and persisted through the shared Mongo codec/repository path.
- [ ] Every Mongo query has an explicit tested index and minimal projection.
- [ ] No direct HTTP/Mongo/Temporal/cache/lock client bypasses Samsepiol library abstractions.
- [ ] Capability factory uses interface-exposed enum keys and an immutable validated `EnumMap`.
- [ ] `/capabilities` omits unavailable cards; individual unavailable capability is exact bodyless 204.
- [ ] Public visitor request path makes zero external-vendor calls.
- [ ] Snapshot freshness/staleness and `ETag` behaviour are deterministic and tested.
- [ ] OAuth, cryptography, collector replay, newsletter PII, management isolation, rate limits, and outbound host allow-list tests pass.
- [ ] Temporal workflows/activities orchestrate only and Spring services hold business logic.
- [ ] All controller/service/workflow/entity message types follow required Lombok/Jackson/BSON annotations and `@NonNull` default.
- [ ] `mvn test` and integration/contract/security test profiles pass without real vendor credentials.
