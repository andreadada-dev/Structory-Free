#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPS="$ROOT/.build-deps"

DADACONFIG_REF="a484e764a4ce285c5bbc2ad27e358bc37104421c"
DADAPLATFORM_REF="a70fad0760aeb68e22b45d2e6fa30f291c9d2066"
DADAGUI_REF="1b1944ec05240cdd4db3212695944636428ac3a2"

checkout_pinned_repo() {
  local repository="$1"
  local destination="$2"
  local ref="$3"

  rm -rf "$destination"
  mkdir -p "$destination"
  git -C "$destination" init -q
  git -C "$destination" remote add origin "https://github.com/$repository.git"
  git -C "$destination" fetch --depth=1 origin "$ref"
  git -C "$destination" checkout --detach FETCH_HEAD
}

mkdir -p "$DEPS"
checkout_pinned_repo "andreadada/DadaConfig" "$DEPS/DadaConfig" "$DADACONFIG_REF"
checkout_pinned_repo "andreadada/DadaPlatform" "$DEPS/DadaPlatform" "$DADAPLATFORM_REF"
checkout_pinned_repo "andreadada/DadaGUIRework" "$DEPS/DadaGUIRework" "$DADAGUI_REF"

sed -i 's#<artifactId>spigot</artifactId>#<artifactId>spigot-api</artifactId>#' "$DEPS/DadaConfig/pom.xml"

mvn -B -f "$DEPS/DadaConfig/pom.xml" -DskipTests install
mvn -B -f "$DEPS/DadaPlatform/pom.xml" install
mvn -B -f "$DEPS/DadaGUIRework/pom.xml" -DskipTests install

echo "Pinned Structory Free build dependencies installed successfully."
