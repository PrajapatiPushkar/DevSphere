# 61. Kubernetes Service Networking & Traffic Routing Architecture

* **Status**: Accepted
* **Impacted Components**: `api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`, `k8s/`
* **Date**: 2026-09-02

---

## Context

Following full-system microservice deployment (Lesson 62), DevSphere required a formal, documented networking architecture defining ClusterIP, NodePort, LoadBalancer, Ingress, Kubernetes DNS resolution, and coexistence with Spring Cloud Eureka service discovery.

---

## Decision

1. **Strict ClusterIP Default for Internal Microservices & Infrastructure**:
   - All internal microservices (`config-server`, `service-discovery`, `auth-service`, `user-service`, `api-gateway`) and infrastructure components (`devsphere-mysql`, `devsphere-redis`, `devsphere-kafka`) use `ClusterIP` services.
   - Internal services are not exposed publicly to eliminate unauthenticated external access vectors.

2. **Kubernetes DNS Service Resolution**:
   - Microservices resolve internal dependencies using namespace-qualified Kubernetes DNS names:
     - `http://devsphere-config-server.devsphere.svc.cluster.local:8888`
     - `http://devsphere-service-discovery.devsphere.svc.cluster.local:8761/eureka/`
     - `http://auth-service.devsphere.svc.cluster.local:8081`
     - `http://user-service.devsphere.svc.cluster.local:8082`
     - `devsphere-mysql.devsphere.svc.cluster.local:3306`
     - `devsphere-redis.devsphere.svc.cluster.local:6379`
     - `devsphere-kafka.devsphere.svc.cluster.local:9092`

3. **Coexistence of Eureka and Kubernetes DNS**:
   - Infrastructure addressing (Config Server, Redis, Kafka, MySQL) uses Kubernetes DNS.
   - Dynamic application-level client routing within API Gateway uses Eureka Service Discovery (`lb://DEVSPHERE-AUTH-SERVICE`, `lb://DEVSPHERE-USER-SERVICE`).

4. **Edge Entry Strategy (Ingress, NodePort, LoadBalancer)**:
   - Primary Perimeter Ingress (`ingress.yaml`) forwards HTTP traffic for `api.devsphere.local` directly to `api-gateway` on ClusterIP port 8080.
   - Controlled demonstration manifests provided:
     - `nodeport-gateway.yaml` (NodePort `30080` for direct node testing).
     - `loadbalancer-gateway.yaml` (LoadBalancer port `80` for cloud load balancer provisioning).

---

## Consequences

* **Positive**:
  - Secure internal boundary with zero public database or messaging exposure.
  - Predictable Kubernetes DNS resolution across pods.
  - Clear separation between application-level Eureka discovery and infrastructure Kubernetes DNS.
* **Trade-offs / Future Scope**:
  - Cloud LoadBalancer and NodePort manifests require cluster provider support to assign external IP addresses.
