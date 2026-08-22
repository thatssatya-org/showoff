# satya-portfolio

Planning repository for Satyajit Roy's self-hosted personal portfolio.

The implementation contract, architecture, integration limits, delivery sequence, and acceptance criteria live in [REQUIREMENTS.md](REQUIREMENTS.md).

Implementation authority:

- [Frontend specification](docs/FRONTEND_IMPLEMENTATION_SPEC.md)
- [Backend specification](docs/BACKEND_IMPLEMENTATION_SPEC.md)
- [Vendor credential checklist](docs/VENDOR_CREDENTIALS.md)

## Intent

Replace the existing link hub with a fast, privacy-first portfolio that owns its content and presents selected public activity from GitHub, Spotify, Instagram, YouTube, and LinkedIn without exposing homelab topology or vendor credentials.

## Proposed repository layout

```text
apps/
  web/                    # Astro + TypeScript public frontend
  api/                    # Java / Spring Boot API selected by Samsepiol BOM; sync workers
infra/
  compose/                # production and local Docker Compose manifests
  nginx-proxy-manager/    # virtual-host guidance only; no live secrets
  observability/          # scrape and alert configuration
  docs/                   # LLM-facing implementation and credential contracts
compose.local.yaml         # memory-capped MongoDB + Temporal local infrastructure
.env.example               # variable names only; never commit real values
REQUIREMENTS.md
```

The backend has a strict platform gate: dependency versions come from `samsepiol-bom`, while persistence, HTTP, cache, locks, and Temporal use `samsepiol-library`. Do not use direct fallbacks when the platform artifact is unavailable; publish/install the required library release first.
