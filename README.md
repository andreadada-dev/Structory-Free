# Structory Free

Public repository for the free edition of Structory.

## Current development line

This hardening branch builds **Structory 26.2-SNAPSHOT**.

Modules:

- `structory-core`: shared API/types, registries, time utilities, atomic file helpers and Paper/Folia-aware scheduling used by Free and Premium.
- `structory-free`: the Free Bukkit/Paper plugin and resources.

## Verified Dada dependencies

The build currently targets:

- DadaGUIRework `2.8.1-SNAPSHOT`
- DadaPlatform `26.3`
- DadaConfig `1.6.2`

The GitHub Actions workflow checks out and installs those repositories before building Structory, so CI does not depend on a pre-populated Maven cache.

For a local build, install the matching Dada artifacts in the local Maven repository first, then run:

```powershell
mvn clean verify
mvn clean install
```

`verify` runs the core and plugin tests. `install` also publishes `structory-core:26.2-SNAPSHOT` to the local Maven repository so the matching Structory Premium development branch can compile against the exact same core.

## Compatibility policy

- Java bytecode target: **16**.
- CI verification: **Java 17 and Java 21**.
- Compile API: Paper `1.19.4-R0.1-SNAPSHOT`.
- `plugin.yml` keeps `api-version: 1.13` intentionally for the project's broad legacy compatibility strategy; it is not a claim that every server version from 1.13 onward is automatically certified.
- Version-specific behavior must stay behind the existing compatibility/version abstractions.
- A Minecraft/Paper/Folia version is considered release-certified only after the release smoke matrix has passed on that server line.

## Folia

Structory declares Folia support and routes synchronous work through `SchedulerUtil`/DadaPlatform global, region and entity schedulers. CI includes a source guard that rejects new active uses of the legacy Bukkit scheduler APIs while ignoring comments and string literals.

The guard is an architectural check, not a replacement for runtime testing. Before a stable release, smoke-test at minimum:

1. plugin enable/disable and `/structory reload`;
2. structure create/destroy across chunks;
3. crafting and saved-item persistence;
4. particle/effect scheduling;
5. chunk unload/reload;
6. player/entity interactions on Folia region boundaries.

## Persistence hardening

Saved items and structure instances are written through a temporary file, preserve the previous version as `.bak`, and use an atomic move when the filesystem supports it. This reduces the chance of truncated YAML after a crash or interrupted write.

## CI

`.github/workflows/ci.yml` performs:

- Folia scheduler static guard;
- Dada dependency bootstrap;
- Java 17/21 matrix builds;
- `mvn clean verify` including regression tests.
