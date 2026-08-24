# Self-hosted personal portfolio — requirements and implementation plan

**Owner:** Satyajit Roy (`thatssatya`)
**Status:** Final implementation baseline
**Last finalised:** 2026-08-23 (link hub screenshot and platform contracts reviewed)
**Scope:** A public personal portfolio with a static-first frontend and a Java backend that safely aggregates selected external activity.

## 1. Executive decision

Build a static-first portfolio using **Astro (latest stable), TypeScript, React islands, and Tailwind CSS**, served as immutable files, plus a separate **Java / Spring Boot API governed by the released Samsepiol BOM**. The API owns OAuth, vendor synchronisation, caching, subscriptions, and the small amount of mutable data. It must never make a vendor request in the visitor request path.

This is the best fit for a portfolio in 2026:

- Astro delivers semantic, crawlable HTML and almost no JavaScript for the narrative pages. A client-rendered SPA is needless latency and weaker SEO here.
- React islands are reserved for live cards: GitHub activity, music, social activity, project filters, and the newsletter form.
- Spring Boot keeps the integration surface in Java, supports robust OAuth and scheduling, and runs cleanly in a small container.
- Docker Compose, MongoDB, and the existing reverse-proxy/Tailscale patterns keep the deployment self-hosted and inexpensive.

The public site will be a **curated work record**, not a social-media firehose and not a dashboard for private infrastructure.

### 1.1 Platform engineering baseline

The backend is an instance-configurable product: each self-hosting operator supplies their own approved public profile, content, vendor handles, OAuth credentials/tokens, and enabled capabilities. It is **not** a multi-tenant hosted service; one deployment has one owner and one isolated set of secrets/data. No account, handle, profile URL, provider capability, or content card is hard-coded for Satyajit.

- Use [`thatssatya-org/samsepiol-bom`](https://github.com/thatssatya-org/samsepiol-bom) as the central Maven dependency-management source. Application modules declare no unmanaged versions for dependencies covered by the BOM. Local development is a temporary, explicit exception: Showoff may resolve the installed `0.0.5-BOM-SNAPSHOT` and `0.0.4-LIBRARY-SNAPSHOT` coordinates from the operator's local Maven repository. This exception is for local builds and Compose verification only; a promoted image must use a released, BOM-governed pair.
- The public BOM audit currently declares Java 21/Spring Boot 3.3.4. Do not claim Java 25/current-Spring support in this application until the BOM publishes that upgrade; target the released BOM selected by the operator.
- Use [`thatssatya-org/samsepiol-library`](https://github.com/thatssatya-org/samsepiol-library) for MongoDB, HTTP clients, caching, locks, Temporal, and every other supported infrastructure abstraction. A portfolio module must not bypass it with a new direct client, repository implementation, or competing wrapper. If an integration needs a stack the library does not support, add the abstraction to the library first, then consume its released version here.
- Use MongoDB for all portfolio-owned persistence. Listmonk remains an exception: its upstream application requires its own internal PostgreSQL database; that database is isolated behind Listmonk and is never application persistence.
- Application objects are immutable. Database state changes through tightly bounded persistence operations; Java service/controller/workflow/activity objects are rebuilt, never mutated in place.
- Optimise the wire by default: BSON codecs from the shared library, indexed/filter-projected Mongo reads, compact JSON that omits null fields, conditional HTTP requests, bounded payloads, compression, and cached public read models. No unindexed Mongo query, N+1 fan-out, whole-document read where a projection suffices, or vendor call in the visitor path.

### 1.2 Backend code and workflow conventions

- Internal service requests/responses use `@Value`, `@Builder`, `@Jacksonized`, `@JsonInclude(JsonInclude.Include.NON_NULL)`, and `@JsonIgnoreProperties(ignoreUnknown = true)`. Controller DTOs and Temporal workflow/activity requests/responses use the same serialisation contract. Every required reference field carries Lombok `@NonNull`; absence is represented by the endpoint contract, not a nullable object graph, unless an explicit exception is approved.
- Mongo entities use immutable Lombok/BSON construction consistent with the shared library: `@EqualsAndHashCode(callSuper = true)`, `@Value`, `@SuperBuilder(toBuilder = true)`, `@Jacksonized`, `@JsonInclude(JsonInclude.Include.NON_NULL)`, `@JsonIgnoreProperties(ignoreUnknown = true)`, `@AllArgsConstructor(onConstructor_ = {@BsonCreator})`, and `@NonNull` for required fields. They extend the library `Entity` and declare an unambiguous collection-specific ID prefix. The library codec registry is mandatory.
- Use Temporal through the shared library for workflows that coordinate Mongo persistence with external side effects or need compensation/retry semantics. Workflows and activities only orchestrate deterministic steps; business logic lives in clearly named Spring services invoked by activities. A multi-document/externally visible transition must be durable and recoverable, not an in-memory sequence.
- Prefer small, readable Spring services. When a service gains multiple policies or provider-specific branches, split it using LLD. No explanatory comments: class, method, and request/response names must explain the behaviour.
- Provider and capability implementations use a Strategy interface with an enum returned by that same interface, then a factory builds an immutable `EnumMap` from the injected bean list at startup. This creates an explicit fail-fast registry, avoids string dispatch, and permits a new vendor/capability to be added without changing public orchestration code.

## 2. Discovery: existing presence and content inventory

### 2.1 Existing link hub

`https://bio.link/thatssatya` is a minimal, dark, profile-led link hub for **Satyajit Roy**, with the public descriptor **“Code | Music | Bikes | Cats”**. A supplied visual reference confirms this information architecture:

| Hub element | Observed content | Migration requirement |
| --- | --- | --- |
| Identity masthead | Avatar, `Satyajit Roy`, and `Code | Music | Bikes | Cats` | Preserve the concise identity line; make it editable owner content, not hard-coded decoration. |
| Subscription affordance | Prominent `Subscribe` button in the masthead | Retain a top-of-page subscription action and a dedicated newsletter route; it must lead to the self-hosted double-opt-in flow. |
| Social rail | Spotify, X, Instagram, LinkedIn, GitHub, email, a support/creator-link icon, and YouTube | Retain every verified destination, in this order by default. Use labelled, accessible links rather than icon-only controls; confirm the ambiguous support/creator destination with the owner before publishing. |
| `Must listen` | One large Spotify card: `Shaukeens` | Migrate as an owner-curated featured music card with cover art and a Spotify deep link. Do not infer whether it is an album, artist, playlist, or track until its URL is verified. |
| `Top things right now` | Three cards: `Chat with AI !`, `I'm listening to...` (Spotify), and `Aero India '23` | Treat these as a curated, ordered “right now” collection. Preserve their ability to use a thumbnail, title, destination, and optional source badge. |

The upstream site put a Cloudflare challenge in front of automated requests, so individual card destinations could not be programmatically verified. The screenshot is authoritative for the visible titles and structure; the owner must approve the exact URLs, the seventh social-link destination, and any card metadata before launch—do not guess or silently discard them.

The requested direction and public GitHub profile establish the link categories the replacement must support:

- professional identity and current role;
- GitHub profile, recent public contribution activity, selected repositories, and personal projects;
- Spotify listening, including the existing **Shaukeens** feature, the current `I'm listening to...` link, and a deliberately shared **On Repeat** snapshot;
- Instagram public profile posts/Reels;
- LinkedIn profile, current job/“now” status, and selected posts;
- YouTube channel uploads/Shorts and channel links;
- newsletter subscription;
- homelab and self-hosting work.

### 2.2 Verified public engineering signals

- GitHub profile: `thatssatya`; public organisation activity includes `thatssatya-org/docker-composes`.
- Public profile data identifies Satyajit Roy at Uni Cards, Bengaluru, and links the legacy GitHub Pages site to the link hub.
- The homelab Compose repository shows a constrained, self-hosted estate using Docker Compose, Tailscale, and an existing Nginx Proxy Manager network. It includes Glance, Immich, LiteLLM with PostgreSQL, Open WebUI, NFS, File Nexus, FinTrack, an Actions runner, Wake-on-LAN monitoring, Nginx RTMP, Autoheal, a VPN client, and Tailscale service advertising.

The portfolio must describe those capabilities at a high level. It must not publish IP addresses, Tailscale node names, ports, container IDs, topology, environment variables, raw health output, or a live service inventory.

### 2.3 Content to obtain before launch

Create `apps/web/src/content/site.ts` (or a CMS record) from a short owner-approved source of truth containing:

- approved name, one-line bio, longer bio, avatar, timezone/location precision, and contact route;
- exact existing hub links and their display order, including the social rail and the ambiguous support/creator link;
- a `mustListen` feature and ordered `rightNow` entries, each with title, thumbnail/cover-art licence or source, destination URL, source badge, visibility, and optional expiry;
- canonical URLs/handles for Spotify, Instagram, LinkedIn, YouTube, GitHub, and email;
- selected projects, featured repositories, outcomes, screenshots, and tech tags;
- approved homelab display name, node role labels, service categories, and public-safe statistics;
- newsletter copy, privacy notice, and sender identity;
- a manual “now” status and pinned social posts.

Nothing is published merely because a vendor account or repository is discoverable.

## 3. Product goals and boundaries

### Goals

1. Make Satyajit legible as a backend-focused fintech engineer who values secure, low-latency, self-hosted systems.
2. Turn public engineering activity into a useful, fast portfolio: featured work, recent GitHub signal, curated music, and chosen social updates.
3. Replace a third-party link hub and mailing flow with owner-controlled infrastructure.
4. Give visitors a truthful view of the homelab without turning it into an attack surface.
5. Keep page load fast on a mobile connection and usable with JavaScript disabled.

### Explicit non-goals for v1

- exposing an authenticated homelab dashboard, public Grafana/Glance, Docker socket, service URLs, device names, SSH, Tailscale, or “live” internal health;
- scraping Instagram, LinkedIn, Spotify, or YouTube;
- an open comment system, visitor accounts, a CMS that needs a public admin panel, or direct messages;
- real-time tracking of current listening, exact physical location, or private contribution data;
- sending newsletters from the web process itself.

## 4. Information architecture and public experience

### 4.1 Routes

| Route | Purpose | Rendering |
| --- | --- | --- |
| `/` | Identity, current focus, featured projects, selected live cards, primary links, newsletter | Static HTML with small islands |
| `/projects` | Curated project case studies and GitHub-backed repository cards | Static, filter island optional |
| `/homelab` | Architecture story, safe aggregate snapshot, services-by-category, operating principles | Static plus cached snapshot island |
| `/now` | Owner-curated current work, learning, music, and selected public links | Static plus cached cards |
| `/uses` | Hardware/software/services deliberately shared by the owner | Static |
| `/newsletter` | Subscription confirmation and privacy explanation | Static + form island |
| `/privacy` | Data processing, vendor cards, newsletter, opt-out, analytics policy | Static |
| `/api/*` | Public, cacheable read API and narrowly scoped subscription endpoint | Spring Boot |

### 4.2 Homepage order

1. Compact identity masthead: avatar, name, **“Code | Music | Bikes | Cats”**, an accessible labelled social rail, and a visible Subscribe button.
2. `Must listen`: the owner-curated **Shaukeens** Spotify card, followed by the cached On Repeat snapshot where enabled. Neither is a live playback tracker.
3. `Top things right now`: the ordered curated cards initially seeded with **Chat with AI !**, **I'm listening to...**, and **Aero India '23**. The collection supports a thumbnail, source badge, expiry, and deep link.
4. A current “building / learning” strip. The owner decides what is public.
5. Featured projects—manual case-study metadata wins over repository popularity.
6. Recent GitHub public activity and a contribution heat-map summary.
7. Selected social cards where official APIs are available, with graceful deep links when they are not.
8. Homelab preview: capability categories, safe aggregate capacity/uptime labels, and a link to `/homelab`.
9. Newsletter signup and direct links.

### 4.3 Visual and interaction direction

- Before frontend implementation, follow this repository's `SKILL.md` design process: define the portfolio's concrete audience and single job, create/review a distinctive token system and wireframe, then implement the approved direction. The current repository skill is the local authority for frontend design decisions.
- Dark-first, closely echoing the current black/charcoal hub surface and compact centered card rhythm, but honour system light mode and provide an explicit theme toggle.
- Dense-but-calm engineering aesthetic: command-line accents, restrained motion, readable long-form case studies. No fake terminal, excessive particle field, or client-side animation tax.
- Self-host all fonts and static media. Use system UI fallback and `font-display: swap`.
- Semantic landmarks, keyboard navigation, visible focus, reduced-motion support, colour contrast meeting WCAG 2.2 AA, descriptive alternative text, and no information conveyed by colour alone.
- Every live card shows its source, last refreshed time, and a non-broken fallback link.

## 5. Functional requirements

### FR-1: identity, links, and content

- Render approved profile, links, and contact options from version-controlled content or an internal-only editor.
- Render a masthead subscription CTA; an accessible, labelled social rail; one `must listen` feature; and an ordered `top things right now` collection. These are content types, not bespoke homepage markup.
- Preserve all verified current link-hub destinations, visible labels, and default ordering after the owner approves migration. Seed the current known content: `Shaukeens`; `Chat with AI !`; `I'm listening to...`; and `Aero India '23`.
- Generate canonical URLs, Open Graph/Twitter cards, `Person`, `WebSite`, `ProfilePage`, and `CreativeWork` JSON-LD.
- Provide RSS/Atom for public “now” updates and case studies; no tracking pixels.

### FR-2: projects and GitHub

- Show manually curated project cards with title, problem, outcome, stack, repository/demo links, screenshots, and status.
- Enrich selected cards with GitHub stars, language, latest public commit date, and release information from cached GitHub data.
- Show the last 8 public events, grouped so a multi-commit push does not create visual noise. Render the feed as a timeline with an accessible newest-first/oldest-first sort control; sorting operates over the cached public snapshot and does not refetch GitHub.
- Show a year contribution heat map and total public contribution count. The owner has opted into GitHub-style anonymous private-contribution disclosure: private contributions may be merged into the calendar and total only, labelled as containing private contributions. Never publish a private repository name, organisation, URL, event type, commit/revision, issue/PR, language, title, body, or time more precise than the calendar day. The public event timeline remains public-only.
- Enrich only Easy Fintrack from public GitHub data. File Nexus remains an owner-authored card; its private-repository metadata and activity are not a public API source.
- The GitHub fine-grained personal access token is accepted only through a Tailnet-restricted management `POST` endpoint, encrypted before MongoDB persistence, and fetched/decrypted only inside the scheduled GitHub adapter. There is no credential `GET`, list, echo, OpenAPI example, audit payload, or browser response. The public visitor path reads cached snapshots only.
- Cache GitHub data, respect rate limits/ETags, and continue serving the last known good snapshot on an outage.

### FR-3: Spotify music card

- After owner OAuth consent, display an owner-approved On Repeat snapshot: cover art, track, artist, album, and a Spotify deep link.
- Optionally display recently played/top tracks only if specifically enabled. Default to no real-time “currently listening” signal.
- Refresh the source data server-side, retain a last-good curated snapshot, and give the owner an internal toggle to hide it instantly.
- Do not embed Spotify’s JavaScript or expose access/refresh tokens to a browser.

### FR-4: Instagram

- Display selected recent feed posts and Reels from an owner-controlled Professional Instagram account, including thumbnail, caption excerpt, publish time, media type, and canonical permalink.
- Fetch only through Meta’s official Graph API, after account linking and required permissions/app approval.
- Support manual pinning, hiding, and a pure profile-link fallback. No scraping, no embedding an unaudited third-party feed widget.

### FR-5: LinkedIn

- Show a hand-authored current role/status and a canonical LinkedIn profile link.
- If access to LinkedIn’s official, approved member/content API is granted, show only owner-selected posts from the cached backend feed.
- Default fallback is pinned/manual post cards that deep-link to LinkedIn. Do not promise public-profile or employment scraping—those are not generally available through a public API.

### FR-6: YouTube

- Show recent uploads/Shorts from the approved channel using YouTube Data API v3 (or the channel upload RSS feed where it is sufficient), with thumbnail, title, duration/date, and deep link.
- Treat Community posts as manual/pinned links: the standard public YouTube Data API does not provide a dependable Community-post feed.
- Cache results and use responsive image variants; never autoplay video.

### FR-7: newsletter

- Replace the third-party hub list with self-hosted **Listmonk + PostgreSQL** on an internal Docker network.
- A public `POST /api/v1/newsletter/subscriptions` accepts email, explicit consent text/version, and an optional source. It delegates to Listmonk’s internal API; the browser never sees Listmonk credentials or admin endpoints.
- Require double opt-in, use a signed confirmation token, retain consent evidence/version/time/IP only for the documented retention period, and provide one-click unsubscribe/preference handling.
- Protect sign-up with same-origin checks, an invisible honeypot, proxy/IP rate limits, input validation, and an abuse-safe generic response. Add a self-hostable challenge only if observed abuse warrants it.
- No marketing tracker pixel. Email is PII: do not place it in application logs, traces, exception messages, or analytics.

### FR-8: homelab showcase

- Present a public-safe “systems I run” story: compute classes (Raspberry Pis and an older Windows laptop), network isolation, backup/maintenance principles, and service categories such as media, AI gateway, photo management, file tooling, automation, monitoring, and remote access.
- Provide a deliberately coarse snapshot: e.g. number of nodes/services online, last backup age band, and availability band. Round values and delay publication. The owner chooses every field.
- Phase 4 metrics are produced by a backend-local cron-scheduled collector running on the Tailnet-connected host. It builds one schema-validated, allow-listed aggregate JSON snapshot, persists it as the delayed public cache, and exposes it only through the cacheable public summary API. A visitor request never triggers collection or reaches into the Tailnet. If collection is later separated from the backend process, it must use a signed, replay-protected private route and must not grant public reachability into the Tailnet.
- Do not disclose live resource use, service/container names by default, URLs, addresses, ports, versions, vendor tokens, screenshots containing sensitive data, or raw Prometheus/Docker output.
- Include a static architecture diagram after the owner approves which components are safe to name.

### FR-9: internal control plane

- Provide an owner-only, Tailnet-restricted route or CLI to connect/revoke provider OAuth, force a sync, choose pins, hide a card, edit the “now” status, and view sync failures.
- The first operator surface is `/operator/github`, not part of public navigation or the public API. It is Tailnet-only and submits the GitHub PAT once to an operator-only same-origin proxy alias. It explicitly states that the token is only for cached GitHub sync and is cleared from browser state after every attempt. The connection-card shape is reusable for future OAuth providers, but OAuth is out of scope here.
- The proxy strips caller-controlled forwarding/identity headers, sets one canonical client-address header from its connection peer, and only proxies the private write endpoint after a Tailnet CIDR check. The API accepts that canonical address only when its immediate peer is inside configured `trusted-proxy-cidrs`, then independently checks configured `tailnet-cidrs` before it reads the request body. The API service is never directly published on the public/LAN web surface.
- Every mutation is auditable with timestamp, source, and actor. Never offer public content management.

### FR-10: generic provider profiles and component availability

- Model every integration as an operator-owned `ProviderProfile`: provider type, approved public handle/link, enabled capabilities, encrypted credential reference, refresh policy, and display policy. A profile can be absent, disabled, connected but awaiting approval, stale, or healthy without changing application code or frontend routes.
- Separate a vendor **capability** (for example `GITHUB_ACTIVITY`, `SPOTIFY_ON_REPEAT`, `YOUTUBE_UPLOADS`, `INSTAGRAM_MEDIA`) from its rendered **component**. The public API publishes a cacheable capability manifest containing only enabled, safe-to-display cards and their component type, data endpoint, ordering, source label, and last-refresh metadata. The Astro frontend maps known component types to islands; unavailable capabilities render nothing, never empty error chrome.
- The generic capability read endpoint returns `200 OK` with a typed snapshot only when a profile is enabled and a public-safe snapshot exists. When the profile/capability is missing, disabled, not authorised, deliberately hidden, or has no approved snapshot, return **`204 No Content`** with no response body. Generated frontend clients map this to `null`/absence and continue rendering the page. Do not turn an optional integration into a `404`, `500`, exception trace, or retry storm.
- A provider outage after a valid snapshot exists serves that timestamped last-known-good snapshot with `200 OK`; an actual public API request failure uses a minimal RFC 9457 problem response without tokens, account identifiers, upstream bodies, or stack traces. Rate limiting and validation failures retain their correct status codes.
- New vendors require only: a profile enum value, a capability enum value, a library-supported HTTP/infrastructure dependency, a Strategy bean, snapshot mapper, projected Mongo persistence, and a frontend component registration if a new visual primitive is genuinely needed. Reuse existing components for equivalent card shapes.

## 6. Vendor integration matrix

| Source | Desired public output | Supported approach | Data freshness | Constraint / fallback |
| --- | --- | --- | --- | --- |
| GitHub | featured repo metadata, recent public activity, heat map | REST v3 plus GraphQL `contributionsCollection`; server token only if needed | events 15 min; repos 1 h; heat map daily | Public REST events are short-lived; contribution calendar needs GraphQL and must not leak private counts |
| Spotify | approved On Repeat/top/recent tracks | OAuth 2.0 Authorization Code + PKCE on the server; cache a rendered snapshot | 1–6 h | Scope/production access policy may limit users; personalised On Repeat access needs a tested owner account. Fallback: curated playlist link/card |
| Instagram | recent posts/Reels | Meta Graph API for a linked Professional account and approved scopes | 6 h | No general public-profile API. Fallback: profile link + owner-pinned media |
| LinkedIn | profile, role, selected posts | profile link plus approved official API only when access is granted | manual / 6 h | Public profile/job/post reading is restricted. Fallback: manual “now” and pinned deep links |
| YouTube | uploads/Shorts | Data API v3 upload playlist or channel RSS | 6 h | Community posts are not a dependable standard API resource. Fallback: channel link/pinned cards |
| Listmonk | email subscription | backend-to-internal Listmonk API | immediate | Self-hosted double opt-in; Listmonk never public |
| Homelab | safe aggregate story/status | Tailnet collector → signed private API snapshot | 15–60 min + delayed publish | No reverse proxy into private systems, no live diagnostics |

The capability manifest is vendor-neutral. A self-hosted operator may enable only GitHub and newsletter, or any approved subset; the same frontend responds to the manifest rather than to a build-time list of Satyajit-specific providers.

Provider policies change. Before shipping each connector, verify scopes, app review, rate limits, branding requirements, and permitted storage/display against the vendor’s current official documentation.

## 7. Technical architecture

```text
Visitor
  │ HTTPS, static HTML/assets + same-origin cached JSON
  ▼
Nginx Proxy Manager (public edge; TLS, headers, rate limits)
  ├── web: Astro static files
  └── api: Spring Boot public API
         ├── MongoDB: content, cache snapshots, provider profiles, and audit metadata
         ├── internal Listmonk API + Listmonk's isolated PostgreSQL database
         ├── scheduled provider adapters → GitHub / Spotify / Meta / YouTube / LinkedIn
         └── private Tailnet-only aggregate collector endpoint

Owner / ops ── Tailscale ──► protected operations path
```

### 7.1 Frontend

- **Astro + TypeScript:** static route generation, Markdown/MDX case studies, content collections, image optimisation, RSS, sitemap, and JSON-LD.
- **React 19 islands:** newsletter form, filter controls, and small cached activity widgets. Hydrate only on visibility/interaction.
- **Tailwind CSS:** local build output; component tokens for colour, spacing, and typography. Use accessible primitives where necessary rather than a heavyweight UI suite.
- **OpenAPI-generated client:** derive the tiny public API client from the Spring Boot OpenAPI document; no duplicated hand-written DTOs. The client treats documented `204 No Content` capability responses as `null`, not exceptions.
- **Component registry:** render cards from the backend capability manifest, mapping stable generic component types to small islands. The public build works unchanged for an operator that has not configured Spotify, Meta, LinkedIn, YouTube, or any future optional vendor.
- **Performance budget:** initial HTML/CSS/critical JS under 150 KB compressed excluding images; no third-party analytics/trackers; LCP under 2.5 s on a mid-tier mobile profile; no render-blocking social embeds.

### 7.2 Backend

- **Java/Spring Boot version selected by the released Samsepiol BOM, Maven:** import the Samsepiol BOM and consume the Samsepiol library abstractions for MVC-adjacent integration concerns, MongoDB, HTTP, cache, locks, and Temporal. Use virtual threads only for bounded blocking integration calls; never schedule unbounded work on request threads.
- **MongoDB + shared codec registry:** persist owner content overrides, normalised external snapshots, sync state, provider profiles, consent-audit correlations, and idempotency keys as versioned documents. Define indexes before query code, retrieve only BSON projections required by public read models, and use the library codecs rather than reflection-heavy generic mapping. No speculative document hydration.
- **Provider adapter boundary:** one Strategy interface per generic capability, with its capability enum exposed by the interface and an immutable enum-keyed factory registry built from injected beans. Strict DTO mapping, `ETag`/`If-None-Match` where supported, timeout/retry/backoff/circuit-breaker policy, and a persisted last-success snapshot are mandatory.
- **Cache model:** a visitor reads a projected Mongo snapshot/Caffeine cache only. Scheduled syncs update snapshots out of band. Start without Redis; introduce it through the shared library only when replicas or workload make it necessary.
- **Durable coordination:** use Temporal workflows for Mongo-plus-external transitions such as provider connection/revocation, token rotation, newsletter hand-off, and collector snapshot publication. Workflows and activities orchestrate; Spring services own the business rules and persistence operations.
- **API contract:** OpenAPI 3.1; RFC 9457 problem responses for failures; `204 No Content` for an unconfigured/hidden/empty optional capability; immutable response DTOs; ISO-8601 timestamps; pagination where arrays can grow; `Cache-Control`, compact JSON, compression, and `ETag` headers.
- **Observability:** JSON logs with PII redaction, private `/actuator` endpoints, Prometheus metrics on the internal network, and alerting for failed syncs/token expiry—not raw visitor browsing data.

### 7.3 Public API surface

| Endpoint | Method | Cache target | Notes |
| --- | --- | --- | --- |
| `/api/v1/profile` | GET | 1 day | owner-approved public identity/links |
| `/api/v1/projects` | GET | 1 h | curated projects with cached GitHub enrichment |
| `/api/v1/capabilities` | GET | 1 h | vendor-neutral manifest of enabled public components; missing profiles are omitted |
| `/api/v1/capabilities/{capability}` | GET | 15 min–6 h | typed cached snapshot; `204` when optional capability is not configured, hidden, unauthorised, or empty |
| `/api/v1/activity/github` | GET | 15 min | grouped public activity and contribution summary |
| `/api/v1/music/on-repeat` | GET | 1 h | curated/cached, no live playback |
| `/api/v1/social/{instagram,youtube,linkedin}` | GET | 1–6 h | only sources enabled by owner |
| `/api/v1/homelab/summary` | GET | 1 h | delayed, aggregate, allow-listed fields only |
| `/api/v1/newsletter/subscriptions` | POST | no-store | strict validation/rate limiting/idempotency |
| `/api/v1/healthz` | GET | no-store | liveness only; no dependency or version details |

The provider-specific legacy/readability endpoints above are convenience aliases over the same generic capability services; they never own divergent data-fetching code. The frontend prefers `/capabilities` and its component data endpoints so a self-hosted instance can add/remove vendors without a frontend rebuild. Internal collector/admin endpoints use a separate route, network policy, authentication realm, and audit log. They are not variations of the public API.

## 8. Security, privacy, and resilience requirements

1. **Secrets:** store OAuth client secrets, refresh tokens, webhook keys, database passwords, and Listmonk credentials as Docker secrets or a private secret manager. Never commit `.env`, do not inject secrets into frontend builds, and encrypt stored tokens using versioned AES-GCM envelope encryption with a rotateable key.
2. **OAuth:** use state, PKCE where supported, exact redirect URIs, short-lived signed state cookies, least-privilege scopes, encrypted refresh tokens, revocation, and an internal disconnect action. Do not treat a social account handle as proof of ownership.
3. **Edge:** TLS 1.3, HSTS after domain validation, CSP with no arbitrary third-party scripts, `frame-ancestors 'none'`, `nosniff`, referrer policy, and proxy-level body/request rate limits. Public edge access is limited to `/api` and static site traffic. The unlinked `/operator/github` surface and its token proxy alias are separately Tailnet CIDR restricted; the proxy clears forwarded headers and emits the sole canonical client address consumed by the API.
4. **Input handling:** Bean Validation with size caps and email normalisation; allow-list outbound vendor hosts; reject unexpected JSON fields; encode all text; use prepared queries; no user-controlled URL fetches.
5. **Data minimisation:** collect only newsletter email + consent evidence when the newsletter is enabled. The GitHub publication projection may retain only the approved public activity snapshot and an anonymous private-contribution calendar/total; it must not persist or emit private repository/event metadata. Do not install behavioural analytics. Use privacy-friendly aggregate server logs with a short documented retention window.
6. **Homelab isolation:** no Docker socket, NPM admin, Listmonk admin, database, Actuator, or private service route may be published through the portfolio vhost. The aggregate collector is outbound/private, signed, replay-protected, and field-allow-listed.
7. **Availability:** each connector has a hard timeout, bounded retries with jitter, circuit breaking, and stale-while-revalidate output. A vendor outage becomes a “last updated” card—not a slow or broken homepage.
8. **Backups:** encrypted MongoDB backups, off-device copy, restore test cadence, and an owner runbook. Backups must include the isolated Listmonk PostgreSQL database and application configuration but can exclude recoverable external cache snapshots when recovery does not need them.
9. **Supply chain:** pinned container image digests in production, SBOM generation, dependency updates, Trivy/Grype image scanning, Semgrep/CodeQL, and CI secret scanning. Do not use `:latest` in the portfolio stack.

## 9. Data model (minimum)

| Mongo collection / entity | Purpose | Sensitive fields |
| --- | --- | --- |
| `site_content` | versioned owner-approved profile, social links, must-listen feature, right-now entries, now text, feature flags | none unless contact copy contains it |
| `project` / `project_link` | curated project descriptions and external links | none |
| `external_snapshot` | normalised capability payload, ETag, fetched/valid-until timestamps, source status, and schema version | potentially provider identifiers |
| `provider_profile` | provider type, public-profile metadata, enabled capabilities, encrypted token envelope, display policy, and refresh state | encrypted credentials only |
| `newsletter_request` | idempotency/consent-audit correlation, not mailing list authority | hashed email/correlation metadata |
| `homelab_summary` | allow-listed, delayed aggregate snapshot | no topology or device identifiers |
| `audit_event` | owner actions and operational changes | actor identity/internal IP, retained narrowly |

Every collection has an explicit ID prefix, a schema version, retention policy, and indexes derived from its query paths. Listmonk remains the subscription system of record. The portfolio MongoDB must not create a second unbounded mailing-list copy.

## 10. Delivery plan

### Phase 0 — content and domain decisions

1. Verify and copy every existing link-hub destination; confirm the support/creator social icon, the URLs/types of `Shaukeens`, `Chat with AI !`, `I'm listening to...`, and `Aero India '23`; choose canonical domain and redirect policy.
2. Approve profile wording—including `Code | Music | Bikes | Cats`—professional disclosure, social handles, featured projects, right-now entries, homelab-safe fields, and privacy text.
3. Create vendor applications only for integrations worth the review/maintenance cost. Start with GitHub, newsletter, and manually curated content.

#### Phase 0 status — 2026-08-23

- [x] Canonical public URL approved: `https://thatssatya.github.io`.
- [x] Redirect policy approved: no legacy redirect is required.
- [x] Public masthead, Bengaluru location disclosure, Instagram-DM contact route, no-behavioural-analytics default, social rail, support link, newsletter sender/consent copy, and the privacy disclosure are owner-approved and implemented in typed static content.
- [x] Owner-approved curated entries are implemented: `Shaukeens`, `Chat with AI!`, Spotify `On Repeat`, and the `Aero India '23` Google Photos album. Spotify is an official embed with a direct fallback link; Aero India uses a labelled carousel and direct album link.
- [x] Remaining legacy destinations resolved: retain X at `https://x.com/thatssatya`; retain YouTube at `https://www.youtube.com/@TheMotoDirector`; retire the legacy email contact link. Do not render an email placeholder.
- [x] Homelab v1 disclosure is approved: publish only the approved title, narrative, service categories, and operating principles. Publish no metrics, diagram, identifiers, topology, live state, or operational detail in v1.
- [x] Featured project content is approved: publish File Nexus as the data-ingestion and transformation platform, and Easy Fintrack as its statement-to-ledger dashboard companion. File Nexus uses the owner-approved repository URL; its availability must be checked before presenting it as a public source record.
- [x] Initial dynamic-integration scope is GitHub only. Spotify, Instagram, LinkedIn, and YouTube remain curated/manual links until separately approved.
- [x] GitHub presentation scope is approved: profile `thatssatya`; eight-event public-only timeline with newest/oldest sorting; public Easy Fintrack enrichment; and a GitHub-style anonymous private-contribution heat map/total. File Nexus stays owner-authored despite its approved repository link.
- [x] GitHub authentication uses a fine-grained PAT stored only as a versioned encrypted MongoDB envelope. It is supplied through a Tailnet-restricted write-only management `POST` and is decrypted only by the internal GitHub sync path.
- [x] Newsletter is deferred. Hide subscription controls and collect no email addresses until the owner approves a public sender domain and self-hosted delivery configuration.
- [ ] Approve a public mail domain and sender identity before enabling the newsletter.

### Phase 1 — static portfolio foundation

1. Scaffold Astro frontend and Spring Boot API as separate apps in this repository. The backend imports `samsepiol-bom` and consumes released `samsepiol-library` modules before any portfolio feature code is added.
2. Implement the semantic homepage, projects, now, uses, privacy, RSS/sitemap/JSON-LD, theme, and accessibility/performance budgets.
3. Add Docker Compose for local development and production, MongoDB with the library codec configuration, reverse-proxy configuration, local health checks, backups, and a staging hostname. Keep Listmonk and its mandated PostgreSQL data volume on an internal-only network.

### Phase 2 — first-party data and GitHub

1. Build the generic provider-profile/capability registry, capability-manifest API, and GitHub Strategy adapter with Mongo snapshots, indexes, projections, ETags, conservative refresh intervals, and owner-curation overrides. Its publication projection exposes eight public events and Easy Fintrack enrichment only; the private-contribution projection is calendar/total-only with no private repository/event metadata.
2. Use the installed local `samsepiol-library:0.0.4-LIBRARY-SNAPSHOT` `token-management` module for local development only. It owns versioned Mongo token envelopes, encryption/decryption, token persistence, and the default-deny management-authorization boundary; application-level `Cipher`/AES-GCM code, token persistence, or ad-hoc bearer guard is prohibited. The Showoff management endpoint accepts only `{ "token": "…" }`; it constructs the token reference, AAD, key ID, and management identity server-side. Expose no credential read endpoint, and let only the scheduled/internal GitHub adapter invoke the library’s callback-only plaintext use path for official GitHub calls. Replace this exception with a released BOM-governed dependency pair before promoting an image.
3. Build GitHub as the only initial dynamic capability. Defer Listmonk, its sender configuration, and all newsletter data collection until a public mail domain is approved.
4. Launch with static/site-content and GitHub only if all checks pass.

### GitHub adapter local snapshot contract

The locally installed `http:0.0.4-LIBRARY-SNAPSHOT` now exposes `HttpResponseEnvelope` through `HttpClient.executeWithResponse(...)`: a single-consumption, bounded body with status and normalized headers. Its per-API request and response diagnostics are disabled by default; when enabled, credential headers are omitted, common JSON secret fields are redacted, and diagnostic payloads are truncated. Showoff uses this contract only in the disabled-by-default scheduled GitHub public-events refresh. It sends `If-None-Match`, persists GitHub's `ETag`, treats `304 Not Modified` as no replacement, and retains the last known good snapshot on any upstream error. No visitor route can trigger this call, and no `WebClient`, vendor SDK, or direct HTTP client is permitted.

Rate-limit parsing, retry policy, quota persistence, and rate-limit-aware scheduling are final-phase technical debt. They belong in a separate shared-library rate-limit module; this HTTP client and Showoff contain no app-side substitute. The current conservative refresh intervals are sufficient, so this work does not gate the GitHub capability.

#### GitHub local delivery status — 2026-08-23

- [x] Private Tailnet-only GitHub PAT setup surface, encrypted library-backed storage, and no-read credential boundary.
- [x] Local HTTP snapshot integration with disabled-by-default redacted diagnostics, bounded responses, ETag persistence, conditional requests, `304` no-write handling, and last-known-good public-event snapshots.
- [x] Local Compose deployment verified with the API and web gateway healthy; GitHub refresh remains disabled until operator credentials and explicit profile approval are configured.
- [x] GraphQL contribution calendar/total public projection: cached anonymous day/count calendar and total only, with explicit private-contribution disclosure approval; no private repository or event metadata is persisted or emitted.
- [x] Easy Fintrack enrichment: cached GitHub GraphQL projection of the explicitly configured public repository only (name, URL, stars, primary language, default-branch commit timestamp, and optional release metadata); private, mismatched, or incomplete responses are rejected and File Nexus remains owner-authored.
- [ ] Replace local snapshot dependencies with released BOM-governed artifacts before promotion.

### Phase 3 — selected media integrations

1. Add Spotify OAuth and owner-approved On Repeat snapshot.
2. Add YouTube uploads/Shorts.
3. Add Instagram only after the account/app permissions are approved; add LinkedIn only if official access is actually granted. Otherwise retain the manual, honest fallbacks.

### Phase 4 — homelab story and operations

1. Define and approve the aggregate homelab schema and diagram. The initial public schema is intentionally empty; metrics are deferred from v1.
2. Implement the backend-local cron-scheduled collector on the Tailnet-connected host. It must construct one validated, allow-listed unified JSON snapshot, persist a delayed cache, and expose it through `/api/v1/homelab/summary`; no visitor request may run the collector. If extraction moves to a separate process, add signed replay-protected private ingestion.
3. Add a Tailnet-only operations interface/CLI, provider token-expiry alerts, Temporal-backed durable workflows for external/Mongo coordination, restore drill, and incident runbooks.

### Phase 5 — promotion hardening and technical debt

1. Replace local snapshot dependencies with released BOM-governed artifacts before promoting an image.
2. Add rate-limit parsing, quota persistence, retry policy, and rate-limit-aware scheduling to the dedicated shared-library rate-limit module only if the current conservative refresh intervals cease to provide sufficient headroom. Do not add an application-side substitute.
3. Add CI hardening—secret, dependency, image, and SAST scanning; unit/integration tests; OpenAPI compatibility; and container health checks—when the service moves beyond the current Tailnet-proxied local deployment or is prepared for promotion.

## 11. Definition of done / acceptance criteria

- [ ] Every old hub destination has been manually verified, retained, redirected, or explicitly retired by the owner.
- [ ] The homepage preserves the existing hub’s identity masthead, top-level Subscribe action, social rail, `Must listen` entry, and ordered `Top things right now` collection—with accessible labels and owner-approved URLs.
- [ ] Lighthouse targets meet 95+ Performance, Accessibility, Best Practices, and SEO on the public homepage under the chosen test profile.
- [ ] Core narrative pages render correctly without JavaScript; live cards degrade to timestamped links.
- [ ] GitHub activity is cached and a vendor outage cannot delay page render or produce a server 500.
- [ ] An unconfigured, disabled, hidden, unauthorised, or empty optional provider capability returns `204 No Content`; the frontend omits its component without an error state. Healthy stale snapshots continue with source/refresh metadata.
- [ ] The public capability manifest and frontend component registry work for an arbitrary self-hosted operator profile; no Satyajit-specific provider account, URL, token, or component list is compiled into the application.
- [ ] Backend dependency versions are governed by `samsepiol-bom`, and MongoDB/HTTP/cache/locks/Temporal consume `samsepiol-library` abstractions only. No direct replacement infrastructure client or unindexed Mongo read exists.
- [ ] Backend DTOs, Mongo entities, Temporal messages, and controller messages meet the immutable Lombok/Jackson/BSON conventions in section 1.2; required object fields use `@NonNull`.
- [ ] Temporal workflows/activities contain orchestration only; durable business rules live in Spring services and recover multi-step Mongo/external transitions.
- [ ] All social integrations use official vendor APIs or clearly marked manual links. No scraping code or unaudited feed widget exists.
- [ ] Spotify output is owner-approved and never exposes current playback by default.
- [ ] Newsletter is self-hosted, double opt-in, unsubscribe-capable, rate-limited, and has passing consent/PII log tests.
- [ ] A port/path scan confirms admin, database, Actuator, Listmonk, Docker, and Tailnet services are not exposed by the public vhost.
- [ ] Homelab output contains only the approved aggregate schema; tests reject identifiers, addresses, ports, URLs, version strings, and arbitrary collector fields.
- [ ] Secrets scanning, dependency/image scanning, SAST, unit tests, integration tests, OpenAPI compatibility checks, and container health checks pass in CI.
- [ ] MongoDB and the isolated Listmonk PostgreSQL restore have been rehearsed on an isolated host.

## 12. Risks and decisions requiring owner input

| Decision | Why it matters | Default recommendation |
| --- | --- | --- |
| Domain and mail domain | DNS, TLS, DMARC/SPF/DKIM, canonical URLs | use a dedicated personal domain; keep mail sending isolated from the web host |
| Newsletter sending provider | Self-hosted Listmonk still needs reputable SMTP delivery | use a transactional SMTP provider or a carefully operated relay; do not run an open SMTP server from the homelab |
| Public hosting location | Portfolio needs public 443 while homelab should stay private | public edge/reverse proxy with only web/API exposed; Tailscale for operations and collectors |
| Meta/LinkedIn app approvals | May not be granted and create long-term compliance work | ship manual deep-link cards first; integrate only after approval |
| Spotify production access | Personalised endpoints and quotas/policies can restrict visibility | use an approved cached playlist snapshot as the resilient fallback |
| Homelab detail | More detail improves novelty but leaks attack surface | publish principles/categories and delayed aggregates only |
| Analytics | Measurement trades against visitor privacy | no behavioural analytics; use privacy-preserving aggregate server metrics only |

## 13. Initial operational checklist

- Register DNS and configure AAAA/A only if IPv6 firewalling is verified; otherwise do not accidentally create a bypass around IPv4 controls.
- Configure TLS certificate renewal, HSTS rollout, CSP report-only validation, backups, and external uptime monitoring before public launch.
- Create separate least-privilege vendor apps; never reuse an all-purpose personal token.
- Put environment-specific values in Docker secrets and publish `.env.example` with names only.
- Pin production images to reviewed digests and run containers as non-root/read-only where compatible.
- Test refresh-token expiry, vendor 429/500, malformed payload, email abuse, database restore, and collector replay scenarios.
- Keep a simple kill switch for every dynamic card; static content must survive when every vendor is down.

---

### Audit notes

- Existing link hub: `https://bio.link/thatssatya` (profile + email-list/double-opt-in states observed; the supplied screenshot confirms the masthead, social rail, `Shaukeens`, `Chat with AI !`, `I'm listening to...`, and `Aero India '23`; individual destinations remain unverified due to the challenge).
- Homelab reference: `https://github.com/thatssatya-org/docker-composes` at commit `5e75886d8ddbea6b710f7f1d38f6a2d953f00aef`.
- GitHub public profile: `https://github.com/thatssatya`.

This document is a requirements baseline. It deliberately does not include provider secrets, private service details, or unapproved public claims.

### Implementation authority

For implementation, this document is read with:

- `docs/VENDOR_CREDENTIALS.md` — exact credential/configuration inventory and secret boundary;
- `docs/FRONTEND_IMPLEMENTATION_SPEC.md` — frontend design, rendering, API, quality, and test contract;
- `docs/BACKEND_IMPLEMENTATION_SPEC.md` — Samsepiol platform, MongoDB, Temporal, generic capability API, security, and TDD contract.

If documents conflict, security/privacy rules win, then the backend/frontend implementation specifications, then this baseline. A vendor feature without official approved access remains a manual deep link/card; it is never replaced by scraping.
