# 68. Production Observability

* Status: Accepted
* Date: 2026-09-04

## Context and Problem Statement

The DevSphere microservices platform operates in a distributed Kubernetes cluster with multiple services (`api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`) and infrastructure dependencies (`MySQL`, `Redis`, `Kafka`). To ensure high availability, fast incident resolution, proactive performance monitoring, and capacity planning, DevSphere requires a production-grade observability foundation.

## Decision Drivers

* Need low-overhead, standardized metric collection across all Spring Boot microservices.
* Must monitor system-level JVM metrics, HTTP request rates, error rates, latency histograms, and infrastructure (Kafka & Redis) activity.
* Observability components (Prometheus & Grafana) must remain internal to the Kubernetes cluster and must not expose sensitive operational metrics or APIs to the public internet.
* Low cardinality metric labeling to prevent memory degradation in Prometheus storage.
* Automated alert rules for critical production conditions with clear severity classification.

## Considered Options

1. **Prometheus & Grafana In-Cluster Deployment (Selected)**
2. External Managed SaaS (Datadog / New Relic / Grafana Cloud)
3. Push-based metrics via custom logging pipelines

## Decision Outcome

Option 1 was chosen. We deploy Prometheus and Grafana internally within the `devsphere` Kubernetes namespace.

### Key Architectural Choices

1. **Prometheus Metrics Endpoint Scrape**:
   - Each Spring Boot microservice exposes `/actuator/prometheus` powered by Micrometer's Prometheus meter registry.
   - Metrics endpoints are restricted to cluster-internal network traffic and are NOT exposed through API Gateway or external Ingress.
   - Prometheus is configured with static cluster-internal Service DNS targets (`api-gateway.devsphere.svc.cluster.local:8080`, etc.) to scrape metrics reliably without requiring cluster-wide RBAC cluster roles.

2. **Custom & Domain Metrics**:
   - Implemented low-cardinality custom business metrics including `devsphere_authentication_attempts_total` (in `auth-service`) and `devsphere_task_operations_total` (in `user-service`).
   - Strict policy against using high-cardinality labels (user IDs, emails, JWT tokens, request body parameters).

3. **Grafana Dashboards**:
   - Grafana is provisioned declaratively with Prometheus as its default datasource (`http://prometheus:9090`).
   - Provisioned an automated production monitoring dashboard (`DevSphere Production Monitoring`) covering HTTP throughput/error rates/P95 latency, JVM heap/threads/GC, Kafka event throughput/duplicates, and Redis cache hit/miss metrics.

4. **Production Alert Rules**:
   - Prometheus alert rules configured in `prometheus-alert-rules` ConfigMap covering: `ServiceUnavailable`, `HighErrorRate`, `HighLatency`, `HighCpuUsage`, `HighMemoryUsage`, `PodRestartCount`, `JvmHeapHigh`, `KafkaConsumerLag`, and `RedisConnectionFailure`.
   - Alert notifications are scoped to Prometheus internal evaluation boundary. External alert routers (Slack/PagerDuty) can be attached seamlessly at the Alertmanager boundary in future iterations.

## Security Considerations

* Prometheus and Grafana run under non-root security contexts (`runAsUser: 65534` for Prometheus, `runAsUser: 472` for Grafana).
* Actuator `/actuator/prometheus` endpoints are strictly unexposed on public ingress pathways.
* Zero secrets or sensitive payload values are present in Prometheus metrics or configuration.

## Consequences

* Positive: Full real-time visibility into HTTP performance, JVM memory health, Kafka event processing, and Redis caching.
* Positive: Zero operational dependency on external third-party SaaS vendors.
* Negative: Prometheus TSDB and Grafana consume internal cluster CPU and memory resources (managed via explicit resource requests and limits).
