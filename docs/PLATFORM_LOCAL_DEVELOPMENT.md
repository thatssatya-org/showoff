# Local Samsepiol platform build

## Status

This workspace has locally installed the current platform source snapshots for
development only. They are not a released dependency pair and must not be used
to satisfy the deployment/release gate in
[`BACKEND_IMPLEMENTATION_SPEC.md`](BACKEND_IMPLEMENTATION_SPEC.md). The owner
has explicitly authorised their use for the current GitHub token-management
integration work only; this does not change the deployment/release gate.

The same local snapshot exception applies to the Tailnet-only Beszel operator
adapter. It must keep token storage in `token-management` and outbound I/O in
the shared bounded `http` client; do not add direct vendor clients or browser
calls to Beszel.

| Source repository | Revision | Installed coordinate |
| --- | --- | --- |
| `platform/samsepiol-bom` | `01721c1` (`master`, no local tag) | `com.samsepiol:bom:0.0.5-BOM-SNAPSHOT` |
| `platform/samsepiol-library` | `44395e5` (`master`, no local tag) | `com.samsepiol.library:*:0.0.4-LIBRARY-SNAPSHOT` |

The BOM declares Java 21 and Spring Boot 3.3.4. The verified toolchain for
this local build is Temurin 21.0.12.1 and Maven 3.9.9. In Showoff, run Maven
through `./mvnw`; it uses these supplied tools directly and does not download a
wrapper distribution.

## Build order

The library parent intentionally has no relative path to the BOM. Install the
BOM into the local Maven repository first, then install the library reactor.
Use a single Maven worker on this host.

```bash
export JAVA_HOME=/home/openclaw/.openclaw/workspace/platform/toolchains/jdk-21.0.12.1+1
export PATH="$JAVA_HOME/bin:/home/openclaw/.openclaw/workspace/platform/toolchains/apache-maven-3.9.9/bin:$PATH"

cd /home/openclaw/.openclaw/workspace/platform/samsepiol-bom
mvn --batch-mode --no-transfer-progress -T 1 install

cd /home/openclaw/.openclaw/workspace/platform/samsepiol-library
mvn --batch-mode --no-transfer-progress -T 1 clean install
```

Artifacts are installed in the default local repository:
`/home/openclaw/.m2/repository`.

## Verified library artifacts

The complete reactor installed the following coordinates at
`0.0.4-LIBRARY-SNAPSHOT`:

```text
ai, cache, cache-core, guava, health, http, kafka, library-application,
library-core, library-root, lock, message-queue, message-queue-core, mongo,
mysql, redis, repository, repository-models, temporal, token-management
```

The portfolio platform contract identifies these relevant modules:
`library-core`, `repository-models`, `mongo`, `http`, `temporal`, `cache-core`
with `guava` for the local cache option, `lock`, and conditionally
`library-application` / `health` only after their security defaults are
reviewed.

## Release gate

Before a non-local build or deployment, replace the local snapshots with the
operator-approved released BOM version and library version from an explicit
Maven repository. Do not invent a version, repository, Java/Spring upgrade, or
direct replacement infrastructure dependency to bypass this gate.
