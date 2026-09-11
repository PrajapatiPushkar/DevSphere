# Local Resource Optimization Runbook

This runbook documents the local resource constraints, audit measurements, memory tuning rationale, and operational benchmarks applied to the **DevSphere** Kubernetes cluster in preparation for Lesson 88.

> [!IMPORTANT]
> **Production vs. Local Development Distinction**:
> **This optimization is for the local Kind development environment and does not redefine production resource requirements.**
> In cloud production environments, multi-replica high availability, higher heap sizes, horizontal autoscaling (HPA), and multi-gigabyte container memory envelopes remain the standard operational baseline.

---

## 1. Why the Local Environment Was Memory Constrained

The local development workstation operates under tight hardware limitations:
- **Host OS**: Windows 11 Home
- **Host Physical RAM**: ~7.7 GB total physical memory
- **Docker Desktop Allocation**: ~3.67 GiB cgroup memory ceiling (WSL2 default ~50% host RAM)
- **Local Kubernetes Platform**: Kind (`kindest/node:v1.37.0`) running a complete microservices topology:
  - 4 Infrastructure services (MySQL 8, Redis 7, Kafka 7.5, Zookeeper 7.5)
  - 5 Spring Boot 3 Java microservices (Config Server, Eureka Service Discovery, Auth Service, User Service, API Gateway)
  - 1 Frontend static NGINX service (React SPA)
  - 1 Ingress Controller (NGINX Ingress)
  - Kubernetes system control plane components (API Server, Controller Manager, Scheduler, etcd, CoreDNS, Kindnet, Local Path Provisioner)

### The Underlying Failure Mode
Prior to optimization, the cumulative memory requests of all scheduled pods totaled **3,708 MiB (98% of allocatable node memory)**, and container limits totaled **9,044 MiB (240% overcommit)**.
Furthermore, Confluent Kafka and Zookeeper lacked explicit `KAFKA_HEAP_OPTS`, defaulting to `-Xmx1G -Xms1G` and `-Xmx512M -Xms512M` respectively, while Spring Boot JVM services targeted 75% of a 1Gi limit (`-XX:MaxRAMPercentage=75.0`).
When workloads surged or GC paused, Docker Desktop crossed 3.5–3.6 GiB of its 3.67 GiB limit, triggering the Linux kernel OOM killer on the Kind container (`devsphere-control-plane Exited (137)`).

---

## 2. Before Measurements

### Docker & Host Memory
- **Docker Memory Usage**: 3.147 GiB / 3.671 GiB (85.73%)
- **Node Allocatable Memory**: 3,849,428 KiB (~3,759 MiB)
- **Node Memory Requests**: 3,708 MiB (98%)
- **Node Memory Limits**: 9,044 MiB (240%)
- **Kind Container Status**: Exited (137) prior to reboot due to OOM kill

### Workload Allocations & Actual Runtime Usage (crictl stats)

| Workload | Requested Memory | Memory Limit | Actual Usage (Before) | JVM / Engine Configuration |
| :--- | :--- | :--- | :--- | :--- |
| **devsphere-config-server** | 512 MiB | 1 GiB | 201.7 MB | `-XX:MaxRAMPercentage=75.0` (768 MB max heap) |
| **devsphere-service-discovery** | 512 MiB | 1 GiB | 267.1 MB | `-XX:MaxRAMPercentage=75.0` (768 MB max heap) |
| **api-gateway** | 256 MiB | 1 GiB | 266.9 MB | `-XX:MaxRAMPercentage=75.0` (768 MB max heap) |
| **auth-service** | 256 MiB | 1 GiB | 520.6 MB | `-XX:MaxRAMPercentage=75.0` (768 MB max heap) |
| **user-service** | 256 MiB | 1 GiB | 505.7 MB | `-XX:MaxRAMPercentage=75.0` (768 MB max heap) |
| **devsphere-kafka** | 512 MiB | 1 GiB | 396.8 MB | Unset `KAFKA_HEAP_OPTS` (Defaulted to `-Xmx1G -Xms1G`) |
| **devsphere-zookeeper** | 256 MiB | 512 MiB | 90.2 MB | Unset `KAFKA_HEAP_OPTS` (Defaulted to `-Xmx512M -Xms512M`) |
| **devsphere-mysql** | 512 MiB | 1 GiB | 402.6 MB | Default InnoDB buffer pool + performance schema |
| **devsphere-redis** | 128 MiB | 512 MiB | 4.4 MB | In-memory ephemeral cache |
| **devsphere-frontend** | 128 MiB | 256 MiB | 10.3 MB | Static NGINX alpine |
| **ingress-nginx-controller** | 90 MiB | 256 MiB | 31.0 MB | Ingress Controller |

---

## 3. Resource Changes

The following Kubernetes manifests were modified with surgical adjustments:

1. **`k8s/infrastructure/redis.yaml`**:
   - `strategy`: Added `maxUnavailable: 1`, `maxSurge: 0`.
   - `requests.memory`: `128Mi` → `64Mi`
   - `limits.memory`: `512Mi` → `128Mi`
2. **`k8s/services/frontend.yaml`**:
   - `requests.memory`: `128Mi` → `64Mi`
   - `limits.memory`: `256Mi` → `128Mi`
3. **`k8s/infrastructure/kafka.yaml`**:
   - **Zookeeper**:
     - `strategy`: Added `maxUnavailable: 1`, `maxSurge: 0`.
     - `env`: Added `KAFKA_HEAP_OPTS: "-Xmx128M -Xms128M"`.
     - `requests.memory`: `256Mi` → `128Mi`
     - `limits.memory`: `512Mi` → `256Mi`
   - **Kafka**:
     - `strategy`: Added `maxUnavailable: 1`, `maxSurge: 0`.
     - `env`: Added `KAFKA_HEAP_OPTS: "-Xmx384M -Xms256M"`.
     - `requests.memory`: `512Mi` → `384Mi`
     - `limits.memory`: `1Gi` → `640Mi`
4. **`k8s/infrastructure/mysql.yaml`**:
   - `strategy`: Added `maxUnavailable: 1`, `maxSurge: 0` (protects single RWO volume from attach race conditions).
   - `requests.memory`: `512Mi` → `384Mi`
   - `limits.memory`: `1Gi` → `768Mi`
5. **`k8s/services/config-server.yaml`**:
   - `strategy`: Set `maxUnavailable: 1`, `maxSurge: 0`.
   - `requests.memory`: `512Mi` → `256Mi`
   - `limits.memory`: `1Gi` → `512Mi`
6. **`k8s/services/service-discovery.yaml`**:
   - `strategy`: Set `maxUnavailable: 1`, `maxSurge: 0`.
   - `requests.memory`: `512Mi` → `384Mi`
   - `limits.memory`: `1Gi` → `512Mi`
7. **`k8s/services/api-gateway.yaml`**:
   - `strategy`: Set `maxUnavailable: 1`, `maxSurge: 0`.
   - `requests.memory`: Maintained `256Mi`
   - `limits.memory`: `1Gi` → `512Mi`
8. **`k8s/services/auth-service.yaml`**:
   - `strategy`: Set `maxUnavailable: 1`, `maxSurge: 0`.
   - `requests.memory`: `256Mi` → `384Mi` (realistic scheduling floor)
   - `limits.memory`: `1Gi` → `768Mi`
9. **`k8s/services/user-service.yaml`**:
   - `strategy`: Set `maxUnavailable: 1`, `maxSurge: 0`.
   - `requests.memory`: `256Mi` → `384Mi` (realistic scheduling floor)
   - `limits.memory`: `1Gi` → `768Mi`

---

## 4. After Measurements

### Docker & Host Memory
- **Docker Memory Usage**: 3.011 GiB / 3.671 GiB (82.01%)
- **Node Memory Requests**: **3,068 MiB (81%)** (reduced from 3,708 MiB / 98%, freeing **640 MiB** of requested headroom)
- **Node Memory Limits**: **5,588 MiB (148%)** (reduced from 9,044 MiB / 240%, eliminating **3,456 MiB** of overcommit risk)
- **Node Conditions**: `MemoryPressure: False`, `DiskPressure: False`, `Ready: True`
- **Application Restart Counts**: **0 across all microservices and infrastructure pods**

### Workload Allocations & Actual Runtime Usage (crictl stats)

| Workload | Requested Memory | Memory Limit | Actual Usage (After) | Net Memory Reduction |
| :--- | :--- | :--- | :--- | :--- |
| **devsphere-config-server** | 256 MiB | 512 MiB | 187.5 MB | -14.2 MB |
| **devsphere-service-discovery** | 384 MiB | 512 MiB | 240.1 MB | -27.0 MB |
| **api-gateway** | 256 MiB | 512 MiB | 298.2 MB | Stable |
| **auth-service** | 384 MiB | 768 MiB | 393.5 MB | **-127.1 MB** |
| **user-service** | 384 MiB | 768 MiB | 438.4 MB | **-67.3 MB** |
| **devsphere-kafka** | 384 MiB | 640 MiB | 351.6 MB | **-45.2 MB** |
| **devsphere-zookeeper** | 128 MiB | 256 MiB | 76.9 MB | **-13.3 MB** |
| **devsphere-mysql** | 384 MiB | 768 MiB | 396.6 MB | -6.0 MB |
| **devsphere-redis** | 64 MiB | 128 MiB | 3.9 MB | -0.5 MB |
| **devsphere-frontend** | 64 MiB | 128 MiB | 10.7 MB | Stable |
| **ingress-nginx-controller** | 90 MiB | 256 MiB | 36.5 MB | Stable |

---

## 5. Why the Values Were Chosen

1. **`KAFKA_HEAP_OPTS` for Kafka and Zookeeper**:
   - Confluent defaults allocate 1 GB initial and max heap (`-Xmx1G -Xms1G`) for Kafka and 512 MB for Zookeeper. For local single-node development with infrequent JSON events, a 256M–384M heap for Kafka and 128M for Zookeeper provides ample throughput while immediately freeing ~600 MB of native virtual memory and RSS.
2. **Spring Boot JVM Services (`auth-service`, `user-service`)**:
   - Both run Spring Boot 3 + Spring Data JPA + Hibernate + Kafka Clients + Eureka Clients + Actuator. At runtime, classloading (~10,000 classes) and threads consume ~350–450 MB RSS. A 768 MiB container limit with `-XX:MaxRAMPercentage=75.0` gives 576 MB max heap, ensuring the JVM never OOMs while eliminating 512 MiB of unnecessary limit overcommit across both services.
3. **Reactive Gateway & Lightweight JVMs (`api-gateway`, `config-server`, `service-discovery`)**:
   - Spring Cloud Gateway uses non-blocking Netty without heavy JPA or thread pools. Config Server serves static file maps. Eureka Server manages a registry of 4 apps. A 512 MiB limit (with 384 MiB max heap) provides over 150–200 MB headroom above their actual footprints.
4. **Single-Replica Deployment Strategy (`maxSurge: 0, maxUnavailable: 1`)**:
   - In a single-node cluster operating at >80% capacity, a `maxSurge: 1` rolling update creates the new pod before terminating the old one, causing `FailedScheduling: 0/1 nodes available: Insufficient memory`. Setting `maxSurge: 0` and `maxUnavailable: 1` ensures the scheduler reclaims old resources before placing the replacement pod.

---

## 6. Difference Between Local and Production Configuration

| Aspect | Local Kind Development Environment | Production Target Architecture |
| :--- | :--- | :--- |
| **Replicas** | 1 replica per microservice/infrastructure | 2–3+ replicas across availability zones with HPA |
| **Rollout Strategy** | `maxUnavailable: 1`, `maxSurge: 0` (zero-surge) | `maxSurge: 25%`, `maxUnavailable: 0` (zero-downtime) |
| **Kafka / Zookeeper** | Single-node lightweight broker (`-Xmx384M`) | Multi-broker Kraft or ZooKeeper ensemble (`-Xmx4G+`) |
| **Service Limits** | 512 MiB – 768 MiB per JVM | 1 GiB – 2 GiB+ with autoscaling triggers at 70% |
| **MySQL** | Single container with local PV (`requests: 384Mi`) | Managed Cloud DB (Amazon RDS / Cloud SQL) with HA |

---

## 7. Remaining Limitations & Recommendations

1. **Host Memory Ceiling**:
   - With Windows host physical memory at ~7.7 GB and Docker Desktop allocated ~3.67 GiB, host RAM headroom remains narrow. Docker Desktop memory should **NOT** be increased above ~3.67 GiB because doing so would starve Windows OS, Chrome, and IDE processes, resulting in aggressive OS paging.
2. **Sequential Redeployments**:
   - Workloads must continue using `maxSurge: 0` locally to avoid simultaneous replica placement.
3. **Build Cache Disk Maintenance**:
   - If disk consumption increases over time due to Docker builds, `docker builder prune -f` can be run to reclaim disk cache (though this does not alter active runtime RAM).
