# Plan: Port OuroMetrics from Folia to Fabric

Status: Implemented
Date: 2026-07-15
Relates to: ADR-001 and ADR-002

## Goal

Add a Fabric server exporter while preserving the platform-neutral metrics port and Prometheus
adapter. Keep the Folia exporter during cutover, retain the `:9940/metrics` scrape contract, and
document a safe consumer wiring pattern for Fabric's shared classloader.

## Scope and constraints

- Reuse `metrics-core` and `metrics-prometheus` without behavior changes.
- Add `fabric-mod`; do not remove `folia-plugin` in this change.
- Target Minecraft 1.21.11, Java 21, Mojang mappings, Fabric Loader 0.19.3, Fabric API
  0.141.4+1.21.11, Loom 1.13.6, and Gradle 8.14.3.
- Keep Prometheus relocated and OuroMetrics API packages unrelocated.
- Porting individual Bukkit/Folia consumers is out of scope.
- The MSPT series is a non-blocking follow-up.

## Platform substitutions

| Concern | Folia | Fabric |
|---|---|---|
| Lifecycle | `JavaPlugin.onEnable/onDisable` | dedicated-server initializer and server lifecycle events |
| Discovery | Bukkit `ServicesManager` | `OuroMetricsApi.registry()` |
| Configuration | `plugins/OuroMetrics/config.yml` | `config/ouro-metrics.properties` |
| Gauge refresh | async scheduler every five seconds | end-of-server-tick callback every 100 ticks |
| Server inventory | online players and loaded plugins | online players and loaded Fabric mods |
| Metadata | `plugin.yml` | `fabric.mod.json` |
| Packaging | Shadow jar | Shadow jar fed into Loom `remapJar` |

## Implementation phases

### 1. Build and configuration

- Register the Fabric module and plugin repository.
- Pin platform versions in `gradle.properties` and update the wrapper for Loom compatibility.
- Add Loom, Shadow, Fabric, Minecraft, Prometheus, test, and publishing configuration.
- Implement strict properties parsing with first-run default creation and unit tests.

Exit criterion: `:fabric-mod:test` passes.

### 2. Exporter shell

- Publish one `PrometheusMetricsRegistry` through `OuroMetricsApi` during server initialization.
- Optionally register JVM metrics, then start the HTTP server immediately for consumer init.
- Register players-online and mods-loaded gauges when enabled.
- Refresh gauges on the server thread every 100 ticks and close the endpoint during shutdown.
- Add server-only Fabric metadata with hard platform dependencies.

Exit criterion: Fabric sources compile and API lifecycle tests pass.

### 3. Consumer and operations continuity

- Document hard-dependency and optional guarded-hook patterns.
- Publish the Fabric exporter to Maven Local for non-transitive compile-only use.
- Make `MetricsRegistry` javadoc platform-neutral.
- Add ADR-002 and update observability inventory/dashboard for `mods_loaded`.

Exit criterion: docs describe buildable wiring and the dashboard queries the Fabric gauge.

### 4. Packaging verification

- Build every module.
- Confirm the final Fabric jar is `OuroMetrics-<version>.jar`.
- Confirm `com/ouroboros/metrics/**` remains linkable.
- Confirm Prometheus exists only under `com/ouroboros/metrics/libs/prometheus/**`.
- Confirm the expanded `fabric.mod.json` is valid and contains no `${version}` placeholder.

Exit criterion: full build and jar inspection pass.

### 5. Runtime smoke test

- Start the Loom dedicated server and accept the local development EULA.
- Verify default config creation and `:9940/metrics` output.
- Verify JVM and `ouro_server_*` series, then exercise a scratch `forPlugin("smoke")` metric.
- Override the port and verify the endpoint moves.
- Stop the server and confirm the exporter releases the port.

Exit criterion: endpoint, config, metric exposure, and clean shutdown are observed, or any
environmental limitation is recorded with the remaining manual check.

## Residual risks

- Shadow-to-remap task wiring is the highest-friction build boundary and must stay covered by jar
  inspection.
- Hard dependencies intentionally make exporter availability part of private consumer startup.
- Prometheus/Grafana host placement, bot routing, and alerting remain the open operational
  questions recorded in ADR-001.
