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
- Explicit runtime adapter matrix: **Minecraft/Bukkit 1.17 through 1.21**.
- 1.17/1.18 use `Version1718`; 1.19/1.20/1.21 use `VersionLatest`.
- Outside that matrix Structory logs a warning and uses `VersionDefault` as a best-effort fallback; that is not a support claim.
- `plugin.yml` keeps `api-version: 1.13` intentionally for the project's broad legacy compatibility strategy; it is not the certified support floor.
- A Minecraft/Paper/Folia version is release-certified only after the release smoke matrix has passed on that server line.

Runtime diagnostics expose the detected Bukkit version, selected compatibility adapter and whether the version is explicitly supported.

## Folia and scheduler contract

Structory declares Folia support and routes scheduled work through `SchedulerUtil`/DadaPlatform. Active plugin code must choose the execution context explicitly:

- `SchedulerUtil.global(...)` for server-global state and global commands;
- `SchedulerUtil.region(location, ...)` for block/world work owned by a region;
- `SchedulerUtil.entity(entity, ...)` for entity/player-owned work;
- `SchedulerUtil.async(...)` for file I/O or CPU work that does not touch live Bukkit world/entity state.

The async executor now starts with `SchedulerUtil.init()` and is no longer owned by config reload. Scheduled global/region/entity tasks are tracked and cancelled during shutdown, while async tasks expose submitted/completed/failure diagnostics.

CI rejects direct Bukkit scheduler APIs, raw executor factories, raw `CompletableFuture.*Async` calls and ambiguous legacy `SchedulerUtil.*Sync` aliases outside the scheduler abstraction itself.

Before a stable release, smoke-test at minimum:

1. plugin enable/disable and `/structory reload`;
2. structure create/destroy across chunks;
3. crafting and saved-item persistence;
4. particle/effect scheduling;
5. chunk unload/reload;
6. player/entity interactions on Folia region boundaries.

## Runtime diagnostics

Operators with `structory.cmd.performance` can run:

```text
/structory performance
```

The command reports platform/Folia mode, compatibility adapter, tracked global/region/entity tasks, scheduler executions/failures, async active/queued/tracked work and submitted/completed/failed async counts.

## Persistence hardening

Saved items and structure instances are written through a temporary file, preserve the previous version as `.bak`, and use an atomic move when the filesystem supports it. Disk persistence is dispatched to the async executor instead of blocking Folia region threads.

Reload also resets runtime crafting and particle caches before configurations and instances are rebuilt, rather than manually invoking Bukkit's `onDisable()`/`onEnable()` lifecycle callbacks.

## Stable release gate

The development line intentionally uses snapshots. Stable `v*` tags are guarded by `.github/workflows/release-gate.yml`; publication fails while mutable non-server `-SNAPSHOT` coordinates remain or a Maven `systemPath`/`system` dependency is introduced. Paper/Spigot API snapshot coordinates are exempt because those repositories use snapshot coordinates as their normal API distribution convention.

## CI

`.github/workflows/ci.yml` performs:

- Folia/scheduler abstraction static guard;
- exact-SHA Dada dependency bootstrap;
- Java 17/21 matrix builds;
- `mvn clean verify` including regression tests for persistence, scheduler lifecycle, event dispatch, command routing, descriptor permissions, saved-item limits, layout orientation and P2 scheduler invariants;
- Maven log artifact upload on every run for failure diagnosis;
- cancellation of superseded branch runs.
