# ouro-metrics

Shared operational metrics for Ouroboros SMP plugins and mods. One Prometheus registry and one
`/metrics` endpoint run per server process; every consumer registers instruments through a
zero-dependency port.

## Modules

| Module | What it is | Who depends on it |
|---|---|---|
| `metrics-core` | The port: `MetricsRegistry`, `PluginMetrics`, `Counter`, `Gauge`, `Timer`. Zero dependencies. | Every instrumented consumer at compile time |
| `metrics-prometheus` | Prometheus adapter behind the port | Exporter modules only |
| `fabric-mod` | Fabric server exporter: owns the registry, serves `/metrics`, and publishes `OuroMetricsApi` | Fabric consumers at compile time; the server at runtime |
| `folia-plugin` | Folia exporter: owns the registry, serves `/metrics`, and registers a Bukkit service | Folia consumers at runtime |

The Fabric and Folia exporters coexist during the server cutover and reuse the same core and
Prometheus adapter. A future platform adapter can implement the same port without changing
instrumentation code.

## Build and publish

```text
./gradlew build                 # compiles all modules, runs tests, and builds both exporter jars
./gradlew publishToMavenLocal   # exposes compile-only APIs to consumer repositories
```

The deployable jars are produced separately at
`fabric-mod/build/libs/OuroMetrics-<v>.jar` and
`folia-plugin/build/libs/OuroMetrics-<v>.jar`. Consumers resolve local compile-only artifacts from
`mavenLocal()` until a shared package repository replaces it.

## Wiring a private Fabric mod

Private mods hard-depend on the exporter, so Fabric guarantees presence and initialization order.
Add the exporter to the compile-only mod classpath without transitives:

```kotlin
repositories { mavenLocal() }
dependencies {
    modCompileOnly("com.ouroboros:fabric-mod:0.1.0") {
        isTransitive = false
    }
}
```

Declare the runtime dependency in `fabric.mod.json`:

```json
{
  "depends": {
    "ouro_metrics": "*"
  }
}
```

Resolve the plugin facade during the consumer's initializer:

```java
PluginMetrics metrics = OuroMetricsApi.registry().forPlugin("mehen");
```

`modCompileOnly` makes `OuroMetricsApi` and the metrics port visible to the compiler but does not
put them in the consumer jar. Never shade or Jar-in-Jar `fabric-mod` or `metrics-core` into a
consumer: the exporter provides the single runtime copy on Fabric's shared classloader.

## Wiring a public Fabric mod

Public mods must still run when OuroMetrics is absent:

1. Define a small internal telemetry interface and a no-op implementation.
2. Put all `com.ouroboros.metrics` imports in one hook class that implements that interface.
3. Add the same non-transitive `modCompileOnly` dependency used above.
4. Optionally add `"suggests": {"ouro_metrics": "*"}` to `fabric.mod.json`.
5. Only load the hook class after
   `FabricLoader.getInstance().isModLoaded("ouro_metrics")` returns true.

Keeping the guard outside the hook prevents the JVM from resolving optional OuroMetrics classes
when the exporter is not installed.

## Wiring a private Folia plugin

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
MetricsRegistry registry = getServer().getServicesManager().load(MetricsRegistry.class);
PluginMetrics metrics = registry != null ? registry.forPlugin("mehen") : PluginMetrics.noop();
```

Public Folia plugins use the same guarded-hook pattern, with `softdepend: [OuroMetrics]` and a
Bukkit plugin-presence check before loading the hook class.

## Naming convention

- Series use `ouro_<plugin>_<subsystem>_<unit>` in lowercase snake case. The facade adds the
  `ouro_<plugin>_` prefix.
- Counters end in `_total`; timers end in `_seconds` and record seconds.
- Labels must have bounded cardinality. Never use player names, UUIDs, or free-form strings.
- Every consumer exposes `ouro_<plugin>_errors_total{where=...}`.

`MetricNames` enforces identifier and suffix rules.

## Exporter configuration

Fabric reads `config/ouro-metrics.properties` and writes it with defaults on first run:

| Property | Default | Purpose |
|---|---:|---|
| `bind` | `0.0.0.0` | Interface serving `/metrics` |
| `port` | `9940` | Exporter port |
| `jvm-metrics` | `true` | Export standard `jvm_*` metrics |
| `server-metrics` | `true` | Export players-online and mods-loaded gauges every 100 ticks |

Folia continues to read `plugins/OuroMetrics/config.yml` with the equivalent settings. Firewall
the exporter port so only the Prometheus host can reach it.

## Deployment

The Prometheus and Grafana stack lives in `ops/observability/`. Its scrape target remains port
9940 across the platform cutover.
