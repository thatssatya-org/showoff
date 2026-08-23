# showoff

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
compose.local.yaml         # loopback-only local web/API demo plus private infrastructure
.env.example               # variable names only; never commit real values
REQUIREMENTS.md
```

The backend has a strict platform gate: dependency versions come from `samsepiol-bom`, while persistence, HTTP, cache, locks, and Temporal use `samsepiol-library`. Do not use direct fallbacks when the platform artifact is unavailable; publish/install the required library release first.

## Frontend local workflow

The static Astro frontend lives in `apps/web`. Set `PUBLIC_SITE_URL` before a production build; it must be the canonical public HTTPS URL and contains no secret.

```bash
cd apps/web
npm install
npm run dev
```

Before release, run `npm test && npm run typecheck && npm run lint && npm run build`. The compiled static site is written to `apps/web/dist/`.

## API local workflow

The API lives in `apps/api`. It targets the Java/Spring versions governed by the
Samsepiol BOM and requires the locally built platform coordinates during
development. Build the BOM and library in the documented order first; see
[local platform development](docs/PLATFORM_LOCAL_DEVELOPMENT.md).

```bash
export JAVA_HOME=/home/openclaw/.openclaw/workspace/platform/toolchains/jdk-21.0.12.1+1
export PATH="$JAVA_HOME/bin:/home/openclaw/.openclaw/workspace/platform/toolchains/apache-maven-3.9.9/bin:$PATH"

cd apps/api
mvn --batch-mode --no-transfer-progress -T 1 test
mvn --batch-mode --no-transfer-progress -T 1 package
```

`compose.local.yaml` keeps MongoDB and Temporal on an internal network. The
API is reachable only through the frontend's same-origin proxy; only the web
container binds to loopback. It expects the packaged API JAR and real
environment-only values; do not put them in `.env.example`. The local API
image is deliberately not a production release artifact: production must use
an operator-approved, released BOM/library pair and pinned image digests.

For an end-to-end local browser path, set `PORTFOLIO_API_ORIGIN` to the API
loopback origin before running Astro in dev or preview mode. Astro proxies only
`/api` in that local server mode; production traffic remains same-origin behind
the reverse proxy.

## Container demo

Build the API JAR first, copy `.env.example` to an untracked `.env`, and fill
the required MongoDB, Temporal PostgreSQL, and public URL variables with
locally generated values. Keep `.env` out of Git.

```bash
cp .env.example .env
# Set MONGO_INITDB_ROOT_USERNAME, MONGO_INITDB_ROOT_PASSWORD,
# TEMPORAL_POSTGRES_USER, TEMPORAL_POSTGRES_PASSWORD,
# PORTFOLIO_PUBLIC_BASE_URL, and PUBLIC_SITE_URL in .env.

cd apps/api
mvn --batch-mode --no-transfer-progress -T 1 package
cd ../..
docker compose --env-file .env -f compose.local.yaml up --build --detach
```

Open `http://127.0.0.1:4321`. The frontend proxies `/api` to the backend over
the Docker network. Stop the demo with `docker compose --env-file .env -f
compose.local.yaml down`; append `--volumes` only when discarding local data is
intended.
