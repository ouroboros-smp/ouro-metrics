# ouro-metrics

Shared operational metrics for Ouroboros SMP plugins. One Prometheus registry and one `/metrics`
endpoint per server process; every plugin registers its own instruments through a zero-dependency
port.

## Modules

| Module | What it is | Who depends on it |
|---|---|---|
| `metrics-core` | The port: `MetricsRegistry`, `PluginMetrics`, `Counter`, `Gauge`, `Timer`. Zero dependencies. | Every instrumented plugin (`compileOnly`) |
| `metrics-prometheus` | Prometheus adapter behind the port | `folia-plugin` only |
| `folia-plugin` | The `OuroMetrics` server plugin: owns the registry, serves `/metrics`, registers the service | Nobody at compile time; everybody at runtime |

The Minestom port gets its own adapter later. Consumer code never changes.

## Build and publish

```
./gradlew build                 # compiles everything, runs tests, produces OuroMetrics-<v>.jar
./gradlew publishToMavenLocal   # required once so consumer repos can resolve metrics-core
```

Consumers resolve `com.ouroboros:metrics-core:0.1.0` from `mavenLocal()`. Move to GitHub Packages
when CI needs it.

## Wiring a plugin (private, runs only on Ouroboros)

`build.gradle.kts`:

```kotlin
repositories { mavenLocal() }
dependencies { compileOnly("com.ouroboros:metrics-core:0.1.0") }
```

`plugin.yml`:

```yaml
depend: [OuroMetrics]
```

`onEnable`:

```java
MetricsRegistry svc = getServer().getServicesManager().load(MetricsRegistry.class);
PluginMetrics metrics = svc != null ? svc.forPlugin("mehen") : PluginMetrics.noop();
```

Hard `depend` is deliberate for the private plugins: it guarantees class visibility and load order,
and the exporter is always installed on our server. Do not shade `metrics-core` into a consumer
jar; a shaded copy has different class identity and the ServicesManager lookup returns null.

## Wiring a public plugin (WildAnimalBalancer pattern)

Public plugins must run on servers without OuroMetrics, so they cannot reference metrics-core
types from classes that always load. Pattern:

1. Define a tiny internal telemetry interface in the plugin with exactly the methods it needs,
   plus a no-op implementation.
2. Put all `com.ouroboros.metrics` imports in one hook class that implements that interface.
3. `softdepend: [OuroMetrics]`, and only load the hook class after checking
   `getServer().getPluginManager().getPlugin("OuroMetrics") != null`.

## Naming convention (enforced by MetricNames)

- Series: `ouro_<plugin>_<subsystem>_<unit>`, lowercase snake_case. The facade adds the
  `ouro_<plugin>_` prefix; you supply the rest.
- Counters end in `_total` (`ouro_patrol_kills_total`).
- Timers end in `_seconds` and record seconds (`ouro_rooms_scan_duration_seconds`).
- Labels: bounded cardinality only. Biome, reason, outcome are fine. Player names, UUIDs, and
  free-form strings are never label values.
- Errors: every plugin exposes `ouro_<plugin>_errors_total{where=...}`.

## Exporter configuration

`plugins/OuroMetrics/config.yml`: `bind` (default `0.0.0.0`), `port` (default `9940`),
`jvm-metrics`, `server-metrics`. Firewall the port so only the Prometheus host can reach it.

## Deployment

The Prometheus + Grafana stack lives in `ops/observability/`. See the README there for scrape
targets and dashboard provisioning.
