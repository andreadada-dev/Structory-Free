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

CI and local bootstrap do not follow mutable dependency branches. They fetch the exact source revisions recorded in the workflow/scripts, so rebuilding the same Structory commit does not silently pick up a newer Dada commit.

For a local Windows build:

```powershell
.\scripts\bootstrap-build-deps.ps1
mvn clean verify
```

On Linux/macOS:

```bash
bash scripts/bootstrap-build-deps.sh
mvn clean verify
```

The bootstrap recreates `.build-deps/`, checks out the pinned Dada revisions and installs their required Maven artifacts. `verify` runs the core and plugin tests. Use `mvn clean install` afterwards when you explicitly want to install `structory-core:26.2-SNAPSHOT` into the local Maven repository.

## Shared core publishing

The parent POM contains `distributionManagement` for the repository's GitHub Packages Maven registry. `.github/workflows/publish-core.yml` can publish `structory-core` manually or from a `core-v*` tag using the repository `GITHUB_TOKEN`.

The publish job is gated by the same stable dependency check used for releases, so it refuses to publish a stable core while mutable non-server snapshot coordinates remain.

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

Reload also resets runtime crafting and particle caches before configurations and instances are rebuilt, rather than manually invoking Bukkit's `onDisable()`/`onEnable()` lifecycle callbacks.

## Stable release gate

The development line intentionally uses snapshots. Stable `v*` tags are guarded by `.github/workflows/release-gate.yml`; publication fails while mutable non-server `-SNAPSHOT` coordinates remain or a Maven `systemPath`/`system` dependency is introduced. Paper/Spigot API snapshot coordinates are exempt because those repositories use snapshot coordinates as their normal API distribution convention.

## CI

`.github/workflows/ci.yml` performs:

- Folia scheduler static guard;
- exact-SHA Dada dependency bootstrap;
- Java 17/21 matrix builds;
- `mvn clean verify` including regression tests for atomic persistence, scheduler lifecycle, event dispatch, command routing, descriptor permissions, saved-item limits and swappable layout orientation;
- Maven log artifact upload on every run for failure diagnosis;
- cancellation of superseded branch runs.
