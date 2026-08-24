# Frontend implementation specification

**Audience:** implementation LLMs and frontend engineers  
**Status:** final build contract  
**Primary references:** `REQUIREMENTS.md`, `docs/BACKEND_IMPLEMENTATION_SPEC.md`, `docs/VENDOR_CREDENTIALS.md`, and repository-local `SKILL.md`

This document is prescriptive. Build a fast personal portfolio, not a generic SaaS dashboard, social-media wall, terminal cosplay, or a frontend that knows the owner’s vendor credentials. When content, URLs, media rights, or an API response is unknown, render a deliberate approved placeholder/empty state or omit the component; never invent biography claims, vendor posts, contributions, credentials, handles, data, or external links.

## 1. Product definition and rendering model

### 1.1 Concrete subject, audience, and single job

- **Subject:** a public field record of a self-hosting, security-conscious backend engineer—code, music, bikes, cats, and careful infrastructure.
- **Audience:** a technical recruiter, engineering peer, potential collaborator, or self-hosting enthusiast deciding in under a minute whether to explore work/contact the owner.
- **Single job:** make the owner’s technical judgement and selected work immediately legible, then lead visitors to an approved project, social profile, newsletter signup, or contact route.

### 1.2 Stack and boundaries

- Build `apps/web` using current stable **Astro**, TypeScript strict mode, React islands only where interaction/data refresh is needed, and Tailwind CSS compiled locally.
- Narrative routes are static, semantic HTML. Hydrate an island only on visibility or direct interaction. Never make the homepage depend on JavaScript to explain who the owner is.
- Use a generated TypeScript API client from the backend OpenAPI document. Do not hand-maintain duplicate request/response interfaces beyond a thin UI view-model mapper.
- Browser code calls only the same-origin portfolio API. It does not call GitHub, Spotify, Meta, YouTube, LinkedIn, MongoDB, Temporal, Listmonk, Docker, Tailscale, or a homelab endpoint directly.
- Self-host all fonts/static assets. No analytics, tracking pixels, remote font requests, social script embeds, autoplay media, or client-side secret.
- Use approved static fixture data in development when the API is unavailable. Fixtures must be visibly marked as development-only and cannot enter production builds.

## 2. Design direction

This is the required first-pass design output from the repository’s frontend-design skill. Build it; do not substitute an unrelated template.

### 2.1 Token system

| Token | Hex | Use |
| --- | --- | --- |
| `carbon` | `#141619` | page ground; quiet, not pure-black default |
| `graphite` | `#22272C` | cards, dividers, elevated controls |
| `paper` | `#F3F1EA` | primary type and high-value content |
| `fog` | `#B8C0C5` | metadata and muted explanatory text |
| `signal-blue` | `#8FAEF8` | active link, focus, selected source marker |
| `road-orange` | `#DD815A` | one sparing highlight for active/pinned “now” material |

Typography:

- **Display:** self-hosted `Recursive` variable font, restrained use for the owner name and large case-study titles. Its code-to-human axis suits the subject without reducing the page to a terminal motif.
- **Body:** self-hosted `IBM Plex Sans`, readable long-form text and navigation.
- **Data/utility:** self-hosted `IBM Plex Mono`, only for timestamps, source labels, counts, and status metadata.

Use legal, version-pinned font files with `font-display: swap`, unicode subset where practical, and system fallbacks. Do not fetch a font from Google at runtime.

### 2.2 Layout and signature

The signature is a narrow **signal mast**: a vertical left-edge rail on wide screens that connects content provenance—curated, GitHub, music, social, homelab—without pretending live infrastructure is visible. On small screens it becomes an ordered source strip above the content. It explains information origin and recency; it is not a decorative timeline.

```text
Desktop

signal mast        content field
──────────────     ┌──────────────────────────────────────────┐
curated  ●         │ Satyajit Roy    [subscribe]              │
github   ├──────── │ Code | Music | Bikes | Cats              │
music    ├──────── │ current thesis / contact route           │
social   ├──────── └──────────────────────────────────────────┘
systems  ●         ┌─────────────┐ ┌─────────────┐
                    │ must listen │ │ right now   │
                    └─────────────┘ └─────────────┘
                    ┌────────────────────────────────────────┐
                    │ featured work / GitHub / homelab story  │
                    └────────────────────────────────────────┘

Mobile

[curated] [github] [music] [social] [systems]
owner / thesis / subscribe
must listen
right now
work, activity, homelab, newsletter
```

The rest remains disciplined: generous edge spacing, restrained 1px dividers, modest rounded corners only on card content, no gradient hero, no glow field, no fake shell prompt, no chart theatre, and no arbitrary numbered sections.

### 2.3 Interaction and motion

- The only orchestrated motion is a 150–220 ms reduced-opacity/provenance transition as a card becomes visible; it reinforces source/recency. It is disabled under `prefers-reduced-motion`.
- Link/card hover changes contrast and signal marker only. Do not translate whole cards or make the interface move constantly.
- Keyboard focus is highly visible using `signal-blue`. Every icon link has an accessible text label/tooltip that works without hover.
- Theme supports system preference plus explicit toggle. The light theme preserves contrast and signal semantics; it is not a washed-out inversion.

### 2.4 Self-critique gate

Before a feature is accepted, check that it does not resemble a generic dark developer portfolio. If the signal mast is absent, if visual language could belong to any dashboard, if a large hero statistic replaces actual work, or if decoration competes with projects, revise. The visual risk is the provenance mast; spend boldness there and keep the rest quiet.

## 3. Information architecture and routes

| Route | Required content | Rendering |
| --- | --- | --- |
| `/` | masthead, social links, subscribe CTA, must-listen, right-now, thesis strip, projects preview, capability cards, homelab preview, newsletter | static shell + visibility islands |
| `/projects` | curated project index/filter and case-study links | static; filter island only if required |
| `/projects/[slug]` | owner-approved case study, outcome, stack, links, media | static Markdown/MDX/content collection |
| `/now` | owner-curated status, selected current material, optional dynamic cards | static + cached capability islands |
| `/homelab` | safe systems story and delayed aggregate only | static + one cached summary island |
| `/uses` | owner-approved hardware/software/services | static |
| `/newsletter` | signup, consent language, confirmation information, privacy link | static + form island |
| `/privacy` | clear data/vendor/newsletter policy | static |
| `/404` | direct recovery to home/projects | static |

Preserve the legacy link-hub information structure: `Code | Music | Bikes | Cats`, Subscribe, accessible social rail, `Shaukeens` must-listen feature, and ordered `Chat with AI !`, `I'm listening to...`, `Aero India '23` cards. Exact URLs/media remain owner content, never assumed.

## 4. Content and component model

### 4.1 Static content source

Use Astro content collections or typed version-controlled content for owner-approved narrative material. Content schemas validate title, summary, slug, visibility, order, image alt text, canonical external URL, source label, and optional expiry. Static content is the baseline when every external API is unavailable.

No raw HTML from vendor captions or Markdown is injected without sanitisation. Do not use `set:html` for external content. Escape strings through normal component rendering.

### 4.2 Generic capability rendering

The frontend learns available dynamic content from:

```text
GET /api/v1/capabilities → CapabilityDescriptorResponse[]
GET descriptor.dataEndpoint → typed snapshot or 204
```

The capability manifest omits unavailable/disabled cards. An explicit capability response of `204 No Content` maps to `null`; do not log it as an error, retry immediately, render an error card, or leave blank chrome. A stale `200` snapshot displays the content with a small, plain `Updated <time>` source label.

Use a closed, typed registry. Example conceptual mapping:

```text
MUSIC_CARD           → MusicCard
ACTIVITY_TIMELINE    → ActivityTimeline
CONTRIBUTION_HEATMAP → ContributionHeatmap
SOCIAL_GRID          → SocialPostGrid
HOMELAB_SUMMARY      → HomelabSummary
```

Unknown component types are ignored with a development-only diagnostic. They must not cause a runtime exception or block surrounding cards. Add a component only when the capability needs a materially different visual grammar; otherwise reuse an existing card.

`REPOSITORY_GRID` is intentionally not a standalone capability card. The selected-work island resolves that descriptor only once, validates the bounded repository snapshot, and renders compact provenance inside a curated project card only when the snapshot repository exactly equals its `publicRepository` identifier. Missing, invalid, failed, or mismatched enrichment renders nothing and never changes the project narrative.

### 4.3 SelectedWork API contract and migration

The future backend-selected-work response must be a bounded presentation contract that the frontend renders directly. Each item contains the curated fields represented by the current project schema: `title`, `summary`, `projectSlug`, `visible`, `order`, `externalUrl?`, `publicRepository?`, `sourceLabel`, `imageAlt?`, `expiresAt?`, `stack`, and the owner-authored case-study body (or an equivalent safe structured narrative). The backend preserves editorial ordering and visibility; the frontend does not re-rank work from stars, activity, or provider popularity.

Curated project narrative is authoritative. Repository provenance is optional enrichment keyed only by exact `publicRepository` equality and limited to the approved public snapshot fields: repository identifier, HTTPS GitHub URL, language, stars, optional release metadata, and cache timestamp/state. The SelectedWork API must not expose or pass through GitHub GraphQL/REST responses, provider-specific DTOs, tokens, private repository data, or other remote-provider payloads. During migration, replace the content-collection load with this response without changing the card contract; retain the current static content as the failure-safe publication source until backend ownership and fallback behaviour are explicitly approved.

### 4.4 Required components

| Component | Inputs | Rules |
| --- | --- | --- |
| `IdentityMasthead` | name, identity line, avatar, social links, subscribe destination | semantic `header`; labelled links; no icon-only controls |
| `SignalMast` | static source categories and enabled capability descriptors | visual provenance only; no private/live state |
| `MustListenCard` | approved feature title, creator, cover, destination | image alt text; Spotify deep link only; never embeds player |
| `RightNowCollection` | ordered approved cards | no invented thumbnails; expired entries are absent; photo previews render only from a same-origin cached snapshot, otherwise the canonical album deep link remains |
| `ProjectCard`/`ProjectGrid` | bounded selected-work model plus optional repository provenance | manual narrative wins over repository popularity; enrichment requires exact identifier match |
| `ActivityTimeline` | grouped cached GitHub events | group noisy commits; source/refreshed metadata |
| `MusicCard` | typed approved snapshot | omit if `null`; no playback/current-listening claim. A labelled Spotify deep link is permitted; in-page playback is deferred. |
| `SocialPostGrid` | selected cached cards | thumbnail/caption excerpt/date/permalink; no social embed scripts |
| `HomelabSummary` | allow-listed delayed aggregate | no hostnames, ports, raw health, exact resources, or private URL |
| `NewsletterForm` | consent copy/version | progressive validation, accessible errors, no PII echo/logging |
| `SourceMetadata` | provider label, refreshed timestamp, stale flag | compact `IBM Plex Mono` utility treatment |

All images use Astro image optimisation or generated responsive variants, width/height to prevent layout shift, `loading="lazy"` except relevant hero media, and decorative images only with empty alt.

## 5. API, form, and failure behaviour

### 5.1 API client rules

- Base URL is same-origin in production. `PUBLIC_API_BASE_URL` may be used only for a local development proxy and contains no secret.
- All `fetch` calls set an abort timeout, use safe cache policy, and do not fan out one request per visible item.
- Dynamic islands receive static/server-fetched props where possible; browser fetch occurs only for isolated refresh/interaction behaviour.
- Handle `200`, `204`, `400`, `429`, and `5xx` exactly as documented. Never parse a `204` response body.
- Do not implement provider-specific fetch clients in the browser.

### 5.2 Newsletter form

`POST /api/v1/newsletter/subscriptions` contains email, consent version, optional source, and honeypot field. The form sends same-origin `fetch` with no analytics or third-party widget.

Success text is generic: “Check your inbox to confirm the subscription.” It reveals neither whether an email already exists nor backend delivery details. Validation errors identify only the field to correct. Rate limiting shows a calm retry-later state. Network failure gives an honest retry action and preserves the typed email only in in-memory component state.

### 5.3 Loading, empty, and error states

- Static content is always visible first.
- Live cards use a small semantic loading placeholder only when the layout area is known; no shimmer animation under reduced motion.
- `204` removes the card/section and closes spacing cleanly.
- A stale `200` card remains visible with timestamp; an API failure does not replace it with upstream error details.
- A full API outage cannot turn the page into skeletons or prevent navigation.

## 6. Accessibility, SEO, privacy, and performance

### 6.1 Accessibility

- Semantic landmarks: skip link, header/nav/main/footer, unique `h1`, logical heading hierarchy.
- Meet WCAG 2.2 AA contrast in both themes. Do not encode provider/status only by colour.
- All controls work by keyboard; focus order follows visual order; visible `:focus-visible`; touch target at least 24×24 CSS pixels and ideally 44×44 for primary controls.
- Respect `prefers-reduced-motion`, `prefers-contrast`, zoom to 200%, and narrow screens at 320 px.
- Form error uses `aria-describedby`/live status appropriately; social icons have text alternatives; images have meaningful alt text.

### 6.2 SEO and sharing

- Static canonical URL, title, descriptive meta description, Open Graph, Twitter card, sitemap, RSS/Atom, and `Person`/`WebSite`/`ProfilePage` JSON-LD.
- Case studies use `CreativeWork` structured data only for real owner-approved material. Do not fabricate dates, ratings, company logos, job titles, or rich-result content.
- Canonical redirects from legacy domain/link destinations are server/edge configuration, not client JavaScript.

### 6.3 Privacy/performance budget

- No tracking pixels, behavioural analytics, cookie banner, remote font/vendor script, or browser vendor token.
- Initial HTML/CSS/critical JavaScript budget: **under 150 KB Brotli/gzip excluding responsive images**. Explain every hydrated island in code review.
- LCP target: under 2.5 seconds on mid-tier mobile. Eliminate layout shift; set image geometry; defer non-critical modules.
- Use content-hashed static assets, cache headers, responsive AVIF/WebP where supported, and a system fallback until self-hosted font preload is safe.

## 7. Frontend test plan

| Layer | Required tests |
| --- | --- |
| Type/schema | content schema rejects missing alt/invalid URL/order; API client `204` maps to `null` |
| Component | masthead labels, capability registry mapping, hidden component on `null`, stale timestamp, newsletter form validation/error text |
| Accessibility | automated axe checks for homepage/newsletter plus keyboard/screen-reader smoke coverage |
| Route | static routes return expected headings/meta/canonical links; no-JS narrative smoke test |
| Browser/E2E | public fixture: all cards; sparse fixture: only static/GitHub; 204 fixture: component absent with no console error; newsletter success/rate-limit/network failure |
| Visual | desktop/mobile screenshots of homepage, card grid, light/dark themes; manual review confirms the signal mast is the sole high-expression element |
| Performance | Lighthouse CI/size-budget check; assert no remote third-party request and no exposed environment secret in built assets |

No frontend test uses a real vendor token, real email address, private social media, or the operator’s homelab. Fixtures contain invented but clearly test-only strings.

## 8. Implementation sequence

1. Scaffold Astro TypeScript/Tailwind project in `apps/web`; configure strict lint/type/test commands and a local development URL.
2. Add token/theme/font infrastructure, semantic shell, skip link, signal mast, static content schemas, and all static routes.
3. Implement identity, link hub migration components, must-listen, right-now, projects, homelab-safe story, and newsletter static form shell using typed content.
4. Add generated-client integration seam, development fixtures, generic capability registry, 204/null behaviour, and source metadata.
5. Add newsletter POST with full state/error/accessibility tests.
6. Add visual/accessibility/performance checks and remove any unapproved remote resource.

The frontend must run with only static/development fixtures while backend credentials are absent. It must not wait for social approval or an API connection to become reviewable.

## 9. Frontend completion checklist

- [ ] `apps/web` starts locally and renders all required static routes with JavaScript disabled for narrative content.
- [ ] The final visual plan uses the defined provenance/signal mast, local token system, and self-hosted fonts; it is not a template/dark dashboard imitation.
- [ ] Legacy hub content structure is preserved with owner-controlled content fields, never guessed URLs.
- [ ] Dynamic rendering uses only the generic capability manifest and generated same-origin API client.
- [ ] `204` yields no card/no error; stale `200` is labelled; unknown component type cannot crash the page.
- [ ] No vendor call, token, database endpoint, private topology value, tracker, remote font, or social script is present in browser code or production bundle.
- [ ] Newsletter form has keyboard-accessible validation and non-enumerating responses.
- [ ] WCAG 2.2 AA, reduced motion, keyboard, mobile, and no-JS checks pass.
- [ ] Lighthouse/asset budget passes and route/component/visual tests pass with fixtures only.
