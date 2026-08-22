# satya-portfolio

Planning repository for Satyajit Roy's self-hosted personal portfolio.

The implementation contract, architecture, integration limits, delivery sequence, and acceptance criteria live in [REQUIREMENTS.md](REQUIREMENTS.md).

## Intent

Replace the existing link hub with a fast, privacy-first portfolio that owns its content and presents selected public activity from GitHub, Spotify, Instagram, YouTube, and LinkedIn without exposing homelab topology or vendor credentials.

## Proposed repository layout

```text
apps/
  web/                    # Astro + TypeScript public frontend
  api/                    # Java 25 / Spring Boot API and sync workers
infra/
  compose/                # production and local Docker Compose manifests
  nginx-proxy-manager/    # virtual-host guidance only; no live secrets
  observability/          # scrape and alert configuration
docs/
  runbooks/
REQUIREMENTS.md
```

No implementation has been started. This commit deliberately establishes the plan first.
