# Prometheus & Grafana Runtime Observability Runbook (Lesson 89)

This runbook documents the architecture, configuration, target discovery, PromQL query catalog, alert rule specifications, operational access procedures, and troubleshooting guidelines for the **DevSphere** runtime observability stack running on the local resource-constrained Kind cluster.

---

## 1. Observability Architecture Overview

The DevSphere observability architecture implements pull-based metrics collection from application microservices to Prometheus, and centralized visualization and dashboarding in Grafana.

```mermaid
flowchart TD
    subgraph Spring Boot Microservices
        AG[api-gateway :8080] -->|/actuator/prometheus| P[Prometheus :9090]
        AS[auth-service :8081] -->|/actuator/prometheus| P
        US[user-service :8082] -->|/actuator/prometheus| P
        CS[config-server :8888] -->|/actuator/prometheus| P
        SD[service-discovery :8761] -->|/actuator/prometheus| P
    end

    subgraph Prometheus Core
        P --> AR["Alert Rules Engine (/etc/prometheus/alerts)"]
        P --> TSDB["Local TSDB (/prometheus)"]
    end

    subgraph Visualization
        G[Grafana :3000] -->|Internal HTTP Proxy: http://prometheus:9090| P
        G --> DB["DevSphere Production Monitoring Dashboard"]
    end

    subgraph Local Access "Secure Port-Forward"
        PF1["kubectl port-forward svc/prometheus 9090:9090"] -.-> P
        PF2["kubectl port-forward svc/grafana 3000:3000"] -.-> G
    end
```

---

## 2. Resource Footprint & Local Environment Sizing

Operating within the constrained local Kind development environment (~7.7 GB host physical RAM, ~3.67 GiB Docker limit, ~3,759 MiB node allocatable):

| Workload | CPU Requests | CPU Limits | Memory Requests | Memory Limits | Storage Type | Security Context |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **prometheus** | `100m` | `500m` | `128Mi` | `256Mi` | `emptyDir: {}` | Non-root (UID 65534), PSS Restricted |
| **grafana** | `50m` | `250m` | `100Mi` | `200Mi` | `emptyDir: {}` | Non-root (UID 472), PSS Restricted |

> [!IMPORTANT]
> **Production vs. Local Development Distinction**:
> In local Kind, Prometheus and Grafana use lightweight ephemeral `emptyDir` volumes to preserve node disk and memory.
> In a production cloud deployment (GKE, EKS, AKS), Prometheus would use persistent volumes (PVC) or Cortex/Thanos for long-term retention, and Grafana would use a PersistentVolume or managed database (PostgreSQL) for session/dashboard persistence.

---

## 3. Local Operational Access Methods

Both Prometheus and Grafana are strictly bound to internal `ClusterIP` services in namespace `devsphere` and are **not** exposed externally via Ingress or public LoadBalancers.

### Port-Forwarding Prometheus (Web UI & API)
To access Prometheus locally on port 9090:
```powershell
kubectl port-forward -n devsphere svc/prometheus 9090:9090
```
- **Web UI**: `http://localhost:9090`
- **Targets Page**: `http://localhost:9090/targets`
- **Rules Page**: `http://localhost:9090/rules`
- **Status/Flags**: `http://localhost:9090/status`

### Port-Forwarding Grafana (Dashboards & Visualization)
To access Grafana locally on port 3000:
```powershell
kubectl port-forward -n devsphere svc/grafana 3000:3000
```
- **Web URL**: `http://localhost:3000`
- **Username**: `admin`
- **Password**: Sourced from secret `devsphere-secrets` (`GRAFANA_ADMIN_PASSWORD` key).

To extract the admin password safely in PowerShell:
```powershell
[System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String((kubectl get secret devsphere-secrets -n devsphere -o jsonpath='{.data.GRAFANA_ADMIN_PASSWORD}')))
```

---

## 4. Scrape Targets & Target Health Verification

Prometheus automatically scrapes the Spring Boot microservices on a 10-second scrape interval via `/actuator/prometheus`:

| Target Job | Internal Kubernetes DNS | Port | Scrape Endpoint | Scrape Status |
| :--- | :--- | :--- | :--- | :--- |
| `api-gateway` | `api-gateway.devsphere.svc.cluster.local` | 8080 | `/actuator/prometheus` | **UP (health: up)** |
| `auth-service` | `auth-service.devsphere.svc.cluster.local` | 8081 | `/actuator/prometheus` | **UP (health: up)** |
| `user-service` | `user-service.devsphere.svc.cluster.local` | 8082 | `/actuator/prometheus` | **UP (health: up)** |
| `config-server` | `config-server.devsphere.svc.cluster.local` | 8888 | `/actuator/prometheus` | **UP (health: up)** |
| `service-discovery` | `service-discovery.devsphere.svc.cluster.local` | 8761 | `/actuator/prometheus` | **UP (health: up)** |

### Verifying Scrape Targets via API
```bash
kubectl exec -n devsphere deployment/prometheus -- wget -q -O - http://localhost:9090/api/v1/targets
```

---

## 5. Grafana Datasource & Provisioned Dashboard

### Datasource Configuration
Grafana automatically provisions the Prometheus datasource via ConfigMap `grafana-datasources`:
- **Datasource Name**: `Prometheus`
- **Datasource Type**: `prometheus`
- **Access Mode**: `proxy`
- **URL**: `http://prometheus.devsphere.svc.cluster.local:9090`
- **Default**: `true`

### Provisioned Dashboard: `DevSphere Production Monitoring`
- **UID**: `devsphere-production-monitoring`
- **Folder**: `DevSphere`
- **Path**: `/etc/grafana/dashboards/devsphere-overview.json`

#### Panels Catalog:
1. **Service Target Status (UP)**: Stat panel showing total number of healthy services (`count(up == 1)`).
2. **Active In-Flight Requests**: Stat panel tracking current in-flight HTTP requests (`sum(http_server_requests_active_seconds_count) by (job)`).
3. **Total HTTP Request Rate**: Stat panel displaying aggregate requests/second across all services (`sum(rate(http_server_requests_seconds_count[1m]))`).
4. **Service Availability Matrix**: Stat panel displaying UP/DOWN status for each individual microservice.
5. **HTTP Request Rate by Service**: Timeseries graph tracking requests per second per microservice.
6. **HTTP 5xx Server Error Rate**: Timeseries graph highlighting server-side failures.
7. **Average Request Latency**: Timeseries graph calculating mean request duration (`sum(rate(seconds_sum)) / sum(rate(seconds_count))`).
8. **Peak Request Latency**: Timeseries graph tracking maximum request duration (`max(http_server_requests_seconds_max)`).
9. **JVM Heap Memory Usage**: Timeseries graph contrasting `jvm_memory_used_bytes{area="heap"}` with `jvm_memory_max_bytes{area="heap"}`.
10. **JVM Non-Heap Memory Usage**: Timeseries graph monitoring Metaspace and code cache memory (`jvm_memory_used_bytes{area="nonheap"}`).
11. **JVM Live Threads**: Timeseries graph tracking thread counts across JVMs (`jvm_threads_live_threads`).
12. **Process CPU Usage**: Timeseries graph tracking CPU percentage consumed by each microservice process (`process_cpu_usage`).
13. **Kafka Consumer Partition Lag**: Timeseries graph tracking Kafka consumer lag per partition and topic (`kafka_consumer_fetch_manager_records_lag_max`).

---

## 6. Real PromQL Query Catalog

| Observability Domain | PromQL Expression | Description |
| :--- | :--- | :--- |
| **Service Availability** | `up{job=~"api-gateway|auth-service|user-service|config-server|service-discovery"}` | Instant gauge: 1 for reachable, 0 for unreachable |
| **Total Request Rate** | `sum(rate(http_server_requests_seconds_count[1m])) by (job)` | Requests per second per service over a 1-minute rate |
| **5xx Server Error Rate** | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m])) by (job)` | Rate of internal server errors |
| **4xx Client Error Rate** | `sum(rate(http_server_requests_seconds_count{status=~"4.."}[1m])) by (job)` | Rate of bad request / unauthorized errors |
| **Average Latency (seconds)**| `sum(rate(http_server_requests_seconds_sum[1m])) by (job) / sum(rate(http_server_requests_seconds_count[1m])) by (job)` | Average duration per HTTP request |
| **Peak Latency (seconds)** | `max(http_server_requests_seconds_max) by (job)` | Maximum latency recorded in the current window |
| **JVM Heap Utilization (%)** | `(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100` | Percentage of allocated heap currently occupied |
| **JVM Live Threads** | `jvm_threads_live_threads` | Current count of live threads in the JVM |
| **Process CPU (%)** | `process_cpu_usage * 100` | Process CPU utilization percentage |
| **Kafka Consumer Lag** | `kafka_consumer_fetch_manager_records_lag_max` | Maximum unconsumed record lag across consumer groups |

---

## 7. Alert Rules Catalog

All alert rules are defined in ConfigMap `prometheus-alert-rules` and loaded into group `devsphere_alerts`:

| Alert Name | Severity | Evaluation Query | Duration | Description |
| :--- | :--- | :--- | :--- | :--- |
| **ServiceUnavailable** | Critical | `up{job=~"..."} == 0` | 1m | Fires when any core microservice target fails to scrape. |
| **HighErrorRate** | Critical | `sum(rate(...{status=~"5.."})) / sum(rate(...)) > 0.05` | 5m | Fires when 5xx errors exceed 5% of total traffic. |
| **HighLatency** | Warning | `sum(rate(seconds_sum)) / sum(rate(seconds_count)) > 2.0` | 5m | Fires when average response latency exceeds 2 seconds. |
| **HighCpuUsage** | Warning | `process_cpu_usage > 0.85` | 5m | Fires when process CPU utilization exceeds 85%. |
| **HighMemoryUsage** | Warning | `jvm_memory_used_bytes / jvm_memory_max_bytes > 0.85` | 5m | Fires when JVM heap memory exceeds 85%. |
| **JvmHeapCritical** | Critical | `jvm_memory_used_bytes / jvm_memory_max_bytes > 0.92` | 3m | Fires when JVM heap approaches exhaustion (>92%). |
| **KafkaConsumerLagHigh** | Warning | `kafka_consumer_fetch_manager_records_lag_max > 100` | 5m | Fires when consumer partition lag exceeds 100 records. |

### Verifying Alert Rules via API
```bash
kubectl exec -n devsphere deployment/prometheus -- wget -q -O - http://localhost:9090/api/v1/rules
```

---

## 8. Troubleshooting Guide

### 1. Prometheus Target Marked as "DOWN"
- **Symptom**: `up{job="<service>"} == 0` or target health displays `DOWN` in Prometheus UI.
- **Root Cause**: The microservice is restarting, not listening on the expected port, or `/actuator/prometheus` is blocked.
- **Resolution**:
  1. Inspect pod status: `kubectl get pods -n devsphere -l app.kubernetes.io/name=<service>`
  2. Test in-pod connectivity:
     ```bash
     kubectl exec -n devsphere deployment/prometheus -- wget -q -O - http://<service>.devsphere.svc.cluster.local:<port>/actuator/prometheus | head -n 5
     ```
  3. Ensure `management.endpoints.web.exposure.include` contains `prometheus`.

### 2. Grafana Reports "Datasource Health Check Failed"
- **Symptom**: Red error banner in Grafana UI: `HTTP error 502 / Connection Refused`.
- **Root Cause**: Grafana is attempting to connect to `localhost:9090` or incorrect DNS name.
- **Resolution**:
  - Verify the datasource URL in `grafana-datasources` ConfigMap is `http://prometheus.devsphere.svc.cluster.local:9090`.

### 3. Prometheus or Grafana Pod Pending Due to "Insufficient Memory"
- **Symptom**: Pod status remains `Pending` and `kubectl describe pod` shows `0/1 nodes are available: 1 Insufficient memory`.
- **Root Cause**: Memory requests exceed node allocatable memory (~3,759 MiB).
- **Resolution**:
  - Ensure Prometheus requests are capped at `128Mi` and Grafana requests at `100Mi` as documented in Section 2.
