# DevSphere Production Observability Architecture

This document specifies the technical design, deployment architecture, metrics instrumentation, alerting rules, security boundaries, and troubleshooting workflows for the DevSphere Production Observability stack.

---

## 1. High-Level Architecture

The DevSphere observability pipeline is built around Prometheus and Grafana running inside the `devsphere` Kubernetes namespace.

```text
DevSphere Microservices
 (api-gateway, auth-service,
  user-service, config-server,
  service-discovery)
        │
        │ internal GET /actuator/prometheus (port 8080, 8081, 8082, 8888, 8761)
        ▼
   Prometheus (prom/prometheus:v2.48.1)
        │
        ├──────────────► Alert Rules ConfigMap (alerts.yml)
        │
        ▼
     Grafana (grafana/grafana:10.2.0)
        │
        ▼
 Dashboards / Production Monitoring (DevSphere Production Monitoring)
```

---

## 2. Spring Boot Actuator & Micrometer Integration

All Spring Boot microservices consume Spring Boot Actuator and the Micrometer Prometheus registry (`micrometer-registry-prometheus`).

### Endpoints Exposure & Internal Security

- Endpoint: `/actuator/prometheus`
- Exposing configuration in `application.yml`:
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: health,info,prometheus
  ```
- **Security Boundary**: The `/actuator/prometheus` endpoint is **NOT** exposed through the public API Gateway or external Kubernetes Ingress. Scrapes occur strictly within the internal Kubernetes virtual network (`devsphere.svc.cluster.local`).

---

## 3. Metrics Categories & Instrumentation

### Application Metrics
- **HTTP Requests**: `http_server_requests_seconds_count`, `http_server_requests_seconds_sum`, `http_server_requests_seconds_bucket` (provides throughput, status code counts e.g., 2xx/4xx/5xx, and latency quantiles).
- **Active HTTP Requests**: `http_server_requests_active_seconds_count`.

### JVM Metrics
- **Memory**: `jvm_memory_used_bytes`, `jvm_memory_max_bytes`, `jvm_memory_committed_bytes` split by area (`heap`, `nonheap`) and memory pools (`G1 Eden Space`, `G1 Old Gen`, etc.).
- **Garbage Collection**: `jvm_gc_pause_seconds_count`, `jvm_gc_pause_seconds_sum`.
- **Threads & Classes**: `jvm_threads_live_threads`, `jvm_threads_daemon_threads`, `jvm_classes_loaded_classes`.

### Process & System Metrics
- **CPU & Process**: `process_cpu_usage`, `system_cpu_usage`, `process_uptime_seconds`, `process_start_time_seconds`.

### Custom Business Metrics
- `devsphere_authentication_attempts_total` (labels: `type` [`login` | `registration`], `status` [`success` | `failure`])
- `devsphere_task_operations_total` (labels: `operation` [`create` | `update` | `complete` | `reopen` | `cancel`])
- `devsphere_tasks_created_total`
- `devsphere_tasks_completed_total`
- `devsphere.user.profile.created.total`

### Kafka Metrics
- `devsphere_kafka_events_processed_total` (labels: `event_type`, `status` [`success` | `duplicate` | `failure`])
- `devsphere_kafka_duplicate_events_total` (labels: `event_type`)
- `kafka_consumer_fetch_manager_records_lag` (when enabled by Spring Kafka client)

### Redis Metrics
- `devsphere_cache_hits_total` (labels: `cache`)
- `devsphere_cache_misses_total` (labels: `cache`)

---

## 4. Metric Cardinality Strategy

To preserve Prometheus TSDB performance and prevent memory exhaustion:
1. **Forbidden Labels**: User IDs, email addresses, JWT tokens, IP addresses, full request URIs with path variables, and exception stack traces MUST NEVER be used as metric labels.
2. **Approved Label Values**: Enums or static categorical values only (e.g., `status: success|failure`, `operation: create|update`, `job: user-service`).

---

## 5. Kubernetes Observability Deployment

Manifests are organized under `k8s/observability/`:
```text
k8s/
└── observability/
    ├── alerts.yaml      # Prometheus Alert Rules ConfigMap
    ├── prometheus.yaml  # Prometheus Deployment, Service, ConfigMap
    └── grafana.yaml     # Grafana Deployment, Service, Datasource & Dashboard ConfigMaps
```

### Prometheus Deployment Specs
- **Replicas**: 1
- **Resource Requests**: CPU `250m`, Memory `512Mi`
- **Resource Limits**: CPU `1000m`, Memory `1Gi`
- **Security Context**: `runAsUser: 65534`, `runAsNonRoot: true`
- **Health Probes**: `/-/healthy` and `/-/ready` on port 9090.

### Grafana Deployment Specs
- **Replicas**: 1
- **Resource Requests**: CPU `250m`, Memory `256Mi`
- **Resource Limits**: CPU `500m`, Memory `512Mi`
- **Security Context**: `runAsUser: 472`, `runAsNonRoot: true`
- **Health Probes**: `/api/health` on port 3000.
- **Datasource Provisioning**: Automatically registers Prometheus (`http://prometheus:9090`) as default.
- **Dashboard Provisioning**: Automatically loads `DevSphere Production Monitoring` dashboard into Grafana.

---

## 6. Production Alerting Rules

Prometheus alert rules are defined in `k8s/observability/alerts.yaml`:

| Alert Name | Condition | Severity | Description |
|---|---|---|---|
| `ServiceUnavailable` | `up == 0` for 1m | `critical` | DevSphere service instance target is unreachable |
| `HighErrorRate` | HTTP 5xx rate > 5% over 5m | `critical` | Elevated 5xx server errors detected |
| `HighLatency` | P95 HTTP latency > 2.0s over 5m | `warning` | P95 request latency exceeds SLA threshold |
| `HighCpuUsage` | `process_cpu_usage > 0.85` for 5m | `warning` | High CPU utilization on service container |
| `HighMemoryUsage` | Heap usage > 85% for 5m | `warning` | Elevated heap memory consumption |
| `PodRestartCount` | Container restarts > 3 in 15m | `warning` | Container crashing or failing probes |
| `JvmHeapHigh` | Heap usage > 90% for 3m | `critical` | Imminent OutOfMemory risk |
| `KafkaConsumerLag` | Consumer lag > 100 or error spike | `warning` | Event processing backlog or duplicate surge |
| `RedisConnectionFailure` | Cache miss surge or Redis down | `warning` | Cache failure or Redis connectivity issue |

---

## 7. Troubleshooting Workflow

1. **Alert Fired**: Check alert notification summary and target `job` label.
2. **Grafana Diagnosis**: Open `DevSphere Production Monitoring` dashboard.
   - Inspect **HTTP Request Rate & 5xx Error Rate** panels to check traffic spikes or failing endpoints.
   - Inspect **P95 Latency** panel to isolate slow downstreams.
   - Inspect **JVM Heap & Threads** panels for memory leaks or thread starvation.
   - Inspect **Kafka & Redis** panels to verify if async event delivery or caching is impaired.
3. **Pod Remediation**: Inspect Kubernetes logs (`kubectl logs -n devsphere deployment/<service-name>`) and check resource consumption against limits.
