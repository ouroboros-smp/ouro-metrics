# Ouroboros observability stack

Prometheus (90d retention) + Grafana with a provisioned "Ouroboros Plugins" dashboard covering
every instrumented plugin plus mehen-bot.

## Bring-up

1. Pick the host (ADR-001 OQ1): the Velocity/MariaDB OVH box (scrapes the world box over the LAN)
   or the Framework Desktop (scrapes over Tailscale; requires the world box on the tailnet).
2. Edit `prometheus/prometheus.yml`: replace `WORLD_BOX` and `BOT_HOST` with real addresses.
3. Change `GF_SECURITY_ADMIN_PASSWORD` in `docker-compose.yml` (local-only credential, still).
4. `docker compose up -d`, then Grafana at `:3000`. The dashboard is in the Ouroboros folder.
5. Firewall: 9940 (world box exporter) and 9941 (mehen-bot) reachable from this host only;
   9090/3000 not exposed publicly.

## Scrape targets

| Job | Target | Source |
|---|---|---|
| minecraft | WORLD_BOX:9940 | OuroMetrics plugin (all ouro_* plugin series, ouro_server_*, jvm_*) |
| mehen-bot | BOT_HOST:9941 | prom-client in mehen-bot (ouro_mehenbot_*, node defaults) |

## Series inventory (as instrumented)

| Plugin | Series | Labels |
|---|---|---|
| server | ouro_server_players_online, ouro_server_plugins_loaded | |
| mehen | ouro_mehen_kicks_total | reason |
| | ouro_mehen_bans_total, ouro_mehen_pardons_total | |
| | ouro_mehen_errors_total | where |
| patrol | ouro_patrol_enforcement_actions_total | action |
| | ouro_patrol_spawns_total | type |
| | ouro_patrol_errors_total | where |
| rooms | ouro_rooms_detections_total | type |
| | ouro_rooms_scan_duration_seconds (histogram) | |
| | ouro_rooms_errors_total | where |
| watershed | ouro_watershed_tick_duration_seconds (histogram) | world |
| | ouro_watershed_cells_changed_total, ouro_watershed_active_bodies | world |
| | ouro_watershed_errors_total | where |
| wab | ouro_wab_spawns_total | species |
| | ouro_wab_balance_pass_seconds (histogram) | |
| | ouro_wab_errors_total | where |
| coffer | ouro_coffer_containers_bound_total, ouro_coffer_access_denied_total | |
| emojibridge | ouro_emojibridge_pack_pushes_total | |
| | ouro_emojibridge_pack_status_total | status |
| | ouro_emojibridge_errors_total | where |
| keepgear | ouro_keepgear_deaths_processed_total | |
| | ouro_keepgear_errors_total | where |
| kinship | ouro_kinship_api_requests_total | op |
| | ouro_kinship_errors_total | where |
| mehenbot | ouro_mehenbot_gateway_events_total | type |
| | ouro_mehenbot_revocations_published_total, ouro_mehenbot_discord_api_errors_total | |
| | ouro_mehenbot_errors_total | where |

Alerting deliberately ships empty (ADR-001 OQ4): let the dashboards prove the series first.
