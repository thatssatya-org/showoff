# Beszel operator-only metrics

`compose.beszel.yaml` is a separate, local-only monitoring project for Docker
container CPU, memory, and disk-I/O history. It is not part of the Showoff web
application, has no public reverse-proxy route, and must never supply direct
metrics, service names, health, topology, or screenshots to a public page.

The public `/homelab` page deliberately renders a static capacity-principles
illustration. Its bars are not connected to Beszel and are explicitly labelled
non-live/public-safe.

## Security boundary

- The hub binds only to `127.0.0.1`; do not change the bind address or place it
  behind the portfolio vhost.
- All three services use the dedicated, Docker-internal `beszel-private`
  network. The hub and agent communicate through a named Unix-socket volume;
  the agent has no TCP listener.
- The agent does **not** receive `/var/run/docker.sock`. A private socket proxy
  receives a read-only bind mount and exposes only the Docker containers API to
  the agent. It has no published port. `CONTAINER_DETAILS=false` also removes
  inspect/log viewing from the hub UI.
- Read-only bind mounts reduce accidental writes but do not make Docker socket
  access harmless by themselves. Keep the proxy private, retain the narrow
  `CONTAINERS=1` allow-list, and review it when Beszel changes its Docker API
  needs.
- All services have CPU, memory, PID, read-only-root-filesystem, tmpfs, and
  `no-new-privileges` limits. Hardware, network-interface, SMART, GPU, and
  systemd collection are disabled to minimise collection and host overhead.
- The two required agent values are mounted as Compose secrets, not passed into
  the agent environment. Keep their real values only in an untracked `.env`, a
  shell environment, or a secret manager. Never commit them.

## Setup

1. Copy `.env.example` to an untracked `.env`, or export the variables from a
   secret manager. Leave the existing portfolio values intact. Set
   `BESZEL_AGENT_PUBLIC_KEY` and `BESZEL_AGENT_TOKEN` only after creating them
   in the local Beszel UI. Keep `BESZEL_APP_URL` loopback-only.
2. Start the hub only and create its first local operator account:

   ```bash
   docker compose --env-file .env -f compose.beszel.yaml up -d beszel
   ```

3. Open `http://127.0.0.1:8090`, create the account, then add a system using
   the Unix socket host value `/beszel_socket/beszel.sock`. Copy the generated
   public key and a universal token into the untracked environment values.
4. Start the remaining local services:

   ```bash
   docker compose --env-file .env -f compose.beszel.yaml up -d
   ```

5. Confirm the agent records only the local Docker services. Do not enable
   public shares, `SHARE_ALL_SYSTEMS`, heartbeat URLs, automatic login, trusted
   authentication headers, or a public proxy route.

To stop the operator stack without deleting retained local history:

```bash
docker compose --env-file .env -f compose.beszel.yaml down
```

Use `down --volumes` only when intentionally discarding the local Beszel
history and pairing state.

## Resource budget and maintenance

The Compose ceilings reserve at most 336 MiB RAM, 0.55 CPUs, and 256 PIDs for
the hub, agent, and socket proxy combined. Normal usage should sit materially
below those ceilings; the limits exist to protect the rest of the homelab.
Images are pinned to Beszel `0.18.8` and socket-proxy `3.4.3-r0-ls92` by
multi-architecture digest. Review release notes, refresh both the version and
digest together, validate the Compose file, and restart the local project only
after an operator-approved update.

Beszel is an operator tool, not the planned public aggregate collector. If a
future public summary is approved, build it as a separate delayed,
schema-validated, field-allow-listed projection; never expose or proxy the
Beszel API.
