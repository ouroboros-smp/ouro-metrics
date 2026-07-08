# ADR-001: Shared metrics architecture for Ouroboros plugins

Status: Accepted
Date: 2026-07-08

## Context

Ten bespoke plugins run (or will run) on the Ouroboros SMP world server, plus the mehen-bot
Discord service. None had telemetry. bStats answers "who runs my public plugin" and is useless for
a fleet that runs on exactly one server; the operational question is "what is each plugin doing
right now and what does it cost." The platform strategy is Folia now, Minestom later, with a
portable hexagonal core, so instrumentation must not bind plugin code to a metrics vendor or a
platform.

## Options

1. Per-plugin bStats. Rejected: reports server counts, not behavior; one server makes it a
   constant.
2. Per-plugin Prometheus endpoint. Rejected: N HTTP servers, N ports, N firewall rules, duplicated
   JVM metrics.
3. Micrometer as the facade. Rejected: a second abstraction layer under our own port buys nothing;
   we control both sides of the port already, and Micrometer widens the dependency surface that
   the exporter plugin has to shade.
4. One shared registry behind a zero-dependency port (`metrics-core`), one exporter plugin
   (`OuroMetrics`) owning the HTTP endpoint, Prometheus java client (`prometheus-metrics` 1.x)
   as the only real dependency, confined to the exporter. Chosen.

## Decision

- `metrics-core` is the port: `MetricsRegistry` -> `PluginMetrics` -> `Counter`/`Gauge`/`Timer`.
  Zero dependencies, Java 21, published to mavenLocal as `com.ouroboros:metrics-core:0.1.0`.
- The `OuroMetrics` Folia plugin owns the `PrometheusRegistry`, serves `/metrics` (default
  `:9940`), exports JVM metrics and `ouro_server_*` gauges, and registers `MetricsRegistry` in the
  Bukkit ServicesManager. Prometheus classes are shaded and relocated; `com.ouroboros.metrics` is
  not relocated because consumers link against it across plugin classloaders.
- Private plugins hard-`depend` on OuroMetrics and call the service directly with a noop fallback.
- Public plugins (WildAnimalBalancer) isolate all metrics-core references in a guarded hook class
  behind `softdepend`, with an internal telemetry interface and noop default.
- Naming is enforced in code: `ouro_<plugin>_...`, counters `_total`, timers `_seconds`, bounded
  label cardinality.
- The Minestom adapter implements the same port later; consumer code is untouched.

## Consequences

- One port to firewall, one scrape target per server process, uniform series naming from day one.
- Consumers carry a compileOnly dependency and nothing at runtime beyond class references.
- mavenLocal is a single-machine publishing story; CI or a second dev box needs GitHub Packages.
- Timers use default Prometheus histogram buckets; tune per-series if a distribution needs it.

## Open questions

- OQ1: Where the Prometheus + Grafana stack runs. Candidates: the Velocity/MariaDB OVH box (same
  LAN as the world server) or the Framework Desktop over Tailscale (requires the world box on the
  tailnet or an exposed, firewalled 9940). `ops/observability` ships with placeholder targets.
- OQ2: Folia TPS/MSPT export. Folia ticks per region; a single TPS gauge misleads. Deferred until
  region-level scheduling metrics are worth the complexity.
- OQ3: mehen-bot scrape path from the Prometheus host (it publishes on :9941).
- OQ4: Alerting rules (Grafana alerting vs Alertmanager). Nothing ships until dashboards prove
  the series are the right ones.
