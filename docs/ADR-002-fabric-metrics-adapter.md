# ADR-002: Fabric metrics exporter adapter

Status: Accepted
Date: 2026-07-15
Relates to: ADR-001 (shared metrics architecture)

## Context

The world server is moving from Folia to Fabric. ADR-001 deliberately isolated platform code in
the exporter while keeping `metrics-core` and `metrics-prometheus` platform-neutral. The exporter
must retain one process-wide registry, one port, JVM metrics, server gauges, and consumer access
without carrying Bukkit APIs into Fabric.

The Folia plugin remains deployable during the cutover. Porting the Bukkit consumer fleet itself
is separate work.

## Decision

- Add `fabric-mod` beside `folia-plugin`; both reuse the existing core and Prometheus adapter.
- Target Minecraft 1.21.11 with Mojang mappings, Fabric Loader, and Fabric API on Java 21.
- Use `DedicatedServerModInitializer` for initialization, Fabric lifecycle events for startup and
  shutdown, and `END_SERVER_TICK` for server-thread gauge refresh every 100 ticks.
- Replace Bukkit `ServicesManager` discovery with `OuroMetricsApi.registry()`. Fabric mods share a
  classloader, and a hard `ouro_metrics` dependency guarantees presence and initialization order.
- Read `config/ouro-metrics.properties`, writing defaults on first use.
- Rename the platform inventory gauge from `ouro_server_plugins_loaded` to
  `ouro_server_mods_loaded`; retaining Bukkit terminology would make the series misleading.
- Flatten the portable modules and Prometheus client into the Fabric jar. Relocate
  `io.prometheus` to `com.ouroboros.metrics.libs.prometheus`, but leave
  `com.ouroboros.metrics` intact for consumers.
- Keep consumers compile-only against the exporter API and never shade or Jar-in-Jar the metrics
  classes into consumer mods.

## Consequences

- The scrape contract remains `:9940/metrics`, so Prometheus targets do not change during cutover.
- Server reads occur on the Minecraft server thread rather than an asynchronous Folia scheduler.
- A malformed configuration stops mod initialization with an actionable error. Failure to bind
  the HTTP port is logged but leaves the registry available to consumers, matching Folia's
  exporter behavior.
- The static accessor is Fabric-specific. Instrument creation remains portable behind
  `MetricsRegistry` and `PluginMetrics`.
- The final artifact must pass through Shadow before Loom remaps it; jar inspection is part of
  release verification.

## ADR-001 OQ2

Fabric has one server tick loop, so a global MSPT series is meaningful. That resolves the
architecture question that made a Folia-wide gauge misleading. Implementing an
`ouro_server_mspt_seconds` series remains a non-blocking follow-up after its exact sampling and
aggregation semantics are selected.
