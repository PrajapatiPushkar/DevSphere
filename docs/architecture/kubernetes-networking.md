# Kubernetes Networking Architecture — Lesson 63

## 1. Overview & Networking Fundamentals

This document details the networking model for the **DevSphere** microservices platform inside Kubernetes.

Kubernetes networking in DevSphere provides:
- **ClusterIP Services**: Internal service-to-service communication.
- **Kubernetes DNS**: Cluster-wide host resolution (`service-name.devsphere.svc.cluster.local`).
- **Eureka Service Discovery**: Application-level dynamic registration and client load balancing coexisting with Kubernetes DNS.
- **Ingress Perimeter Routing**: External client HTTP routing to `api-gateway`.
- **NodePort & LoadBalancer**: Edge connectivity options for direct node testing and cloud load balancer provisioning.

```text
                 Internet / Client
                        │
                        ▼
                 LoadBalancer (Port 80) / Ingress (api.devsphere.local) / NodePort (30080)
                        │
                        ▼
                   API Gateway (ClusterIP: 8080)
                   /         \
                  /           \
                 ▼             ▼
           Auth Service    User Service
           (ClusterIP: 8081) (ClusterIP: 8082)
                 \             /
                  \           /
                   ▼         ▼
              Internal Infrastructure
             /        |        \
            v         v         v
       Config Server Eureka   Redis -> Kafka
       (8888)        (8761)   (6379)   (9092)
```

---

## 2. Kubernetes Service Types

| Service Type | Scope | Usage in DevSphere |
| :--- | :--- | :--- |
| **ClusterIP** | Internal Cluster Only | Default for all microservices (`config-server`, `service-discovery`, `auth-service`, `user-service`, `api-gateway`) and infrastructure (`mysql`, `redis`, `kafka`). |
| **NodePort** | Node IP + Static Port | Controlled testing manifest (`k8s/networking/nodeport-gateway.yaml`) exposing `api-gateway` on static node port `30080`. |
| **LoadBalancer** | Cloud Load Balancer | Controlled cloud manifest (`k8s/networking/loadbalancer-gateway.yaml`) mapping port `80` to `api-gateway` port `8080`. |
| **Ingress** | HTTP/HTTPS Perimeter Router | Primary production edge router (`k8s/ingress/ingress.yaml`) forwarding `api.devsphere.local` to `api-gateway`. |

---

## 3. Internal Service Communication & Kubernetes DNS

Every Kubernetes Service registers a DNS name in CoreDNS. 

### DNS Naming Hierarchy

- **Short Name (Same Namespace)**: `user-service`
- **Namespace-Qualified Name**: `user-service.devsphere`
- **Fully Qualified Domain Name (FQDN)**: `user-service.devsphere.svc.cluster.local`

### DNS Resolution Flow
```text
Pod Container
    │
    ├─► DNS Lookup: "auth-service"
    │      │
    │      ▼
    ├─► CoreDNS resolves FQDN: "auth-service.devsphere.svc.cluster.local"
    │      │
    │      ▼
    ├─► Returns ClusterIP (e.g. 10.96.142.18)
    │      │
    │      ▼
    └─► kube-proxy routes traffic to ready Pod IP (e.g. 10.244.1.14:8081)
```

### DevSphere Internal Service DNS Endpoints

- **Config Server**: `http://devsphere-config-server.devsphere.svc.cluster.local:8888`
- **Service Discovery (Eureka)**: `http://devsphere-service-discovery.devsphere.svc.cluster.local:8761/eureka/`
- **Auth Service**: `http://auth-service.devsphere.svc.cluster.local:8081`
- **User Service**: `http://user-service.devsphere.svc.cluster.local:8082`
- **MySQL Database**: `devsphere-mysql.devsphere.svc.cluster.local:3306`
- **Redis Cache**: `devsphere-redis.devsphere.svc.cluster.local:6379`
- **Apache Kafka Broker**: `PLAINTEXT://devsphere-kafka.devsphere.svc.cluster.local:9092`

> [!IMPORTANT]
> `localhost` and `127.0.0.1` are reserved exclusively for loopback intra-container communication. Inter-service networking must always use Kubernetes DNS names.

---

## 4. Coexistence: Eureka vs. Kubernetes DNS

DevSphere utilizes both Eureka Service Discovery and Kubernetes DNS without conflict:

| Feature | Kubernetes DNS | Eureka Service Discovery |
| :--- | :--- | :--- |
| **Layer** | Infrastructure / Network L4 | Application / Spring Cloud L7 |
| **Primary Use** | Addressing stateful brokers, databases, and bootstrap endpoints (`mysql`, `redis`, `kafka`, `config-server`). | Dynamic client-side load balancing in API Gateway (`lb://DEVSPHERE-AUTH-SERVICE`, `lb://DEVSPHERE-USER-SERVICE`). |
| **Registration** | Automatic via Kubernetes Service selectors. | Spring Cloud `@EnableDiscoveryClient` registration on application startup. |

---

## 5. Network Security & Perimeter Boundaries

- **Publicly Exposed**: Only `api-gateway` (via Ingress, LoadBalancer, or NodePort).
- **Strictly Internal**: `auth-service`, `user-service`, `config-server`, `service-discovery`, `mysql`, `redis`, `kafka`, `zookeeper`.

All internal databases and messaging brokers remain inaccessible from outside the cluster network boundary.

---

## 6. Manifest Validation

Validate all networking manifests statically using `kubectl kustomize`:

```bash
kubectl kustomize k8s/
```
