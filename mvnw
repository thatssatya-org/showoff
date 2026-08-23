#!/usr/bin/env sh
set -eu

REPOSITORY_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAVA_HOME="$REPOSITORY_DIR/../platform/toolchains/jdk-21.0.12.1+1"
MAVEN_HOME="$REPOSITORY_DIR/../platform/toolchains/apache-maven-3.9.9"

if [ ! -x "$JAVA_HOME/bin/java" ] || [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  echo "Showoff's pinned local JDK 21/Maven 3.9.9 toolchain is unavailable under $REPOSITORY_DIR/../platform/toolchains." >&2
  exit 1
fi

export JAVA_HOME
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
exec "$MAVEN_HOME/bin/mvn" --batch-mode --no-transfer-progress -T 1 "$@"
