# Chapter 12. Service mesh.pdf ៖
Chapter 12. Service mesh
한국공학대학교 
TECH UNIVERSITY OF KOREA

Lee youngkon
01. Need for service mesh
A problem situation
Increased microservices $\rightarrow$ Increased communication complexity1
Call path between services unclear2
Difficulty tracking causes in the event of a failure3
Communication between services is key to the system4
Communication problems in MSA environments
Proliferation of Inter-Service REST Calls5
Timeout/retry logic redundancy implementation for each service6
Security (TLS) Direct Implementation Required7
Cascading Failure8
REST / gRPC (timeout: 30ms, timeout: 20ms, timeout: 50ms)9
Microservice Interaction Context (Example Diagram)
Product Management (Core Domain): Products / Categories / Brands (Owner BC)10
Provide Product Master Data11
Provide Products / Categories / Brands12
Procurement (Supporting Domain): Suppliers / Purchase / Purchase Contracts (Owner BC)13
Provide Purchase / Purchase Contract Data14
Provide Receipt / Inspection Results15
Partner Management (Supporting Domain): Partners / Customers / Channels / Distributors (Owner BC)16
Provide Partner / Customer Data17
Provide Target Customer Data18
Commercial Policy (Supporting Domain): Pricing / Promotion / Contract Policy (Owner BC)19
Provide Pricing / Discount / Promotion / Rebates20
Sales Execution (Core Domain): Quotes / Orders / Sales Activities (Owner BC)10
Provide Sales / Order Billing Data21
Provide Partner / Customer / Channel Data22
Provide Customer / Delivery Target Data23
Provide Shipping Request24
Fulfillment (Core Domain): Inventory / Warehouse / Shipping / Delivery (Owner BC)10
Provide Inventory / Delivery / Return Status25
Finance (Core Domain): Invoicing / Settlement (Owner BC)10
Provide Payment / Settlement Data26
Provide Contract / Settlement Data27
Corporate Support (Generic Domain): IAM / Organization / Common Code (Published Language)28
Solution plan
Problem with direct implementation inside the Pod (Retry, timeout, circuit breaker):29
Redundant implementation per service30
Difficulty in maintenance31
Unable to standardize32
Pod Structure: Common Features (Distributed tracking, Data logging, Monitoring, Security/Authentication), App logic33
Infrastructure-level Communication Control Structure:34
Characteristics:35
Operate without modifying application code36
Proxy handles communications37
Istio Mesh Architecture:38
Data plane: Ingress traffic, Mesh traffic, Egress traffic (Service A $\rightarrow$ Proxy $\rightarrow$ Proxy $\rightarrow$ Service B)39
Control plane: Discovery, Configuration, Certificates (istiod: Pilot, Citadel, Galley)40
Service mesh functionality & Key Features
Core Functions: Traffic Control, Distributed Tracking, Data logging, Monitoring, Security, Authentication41
Sidecar Proxy Pattern: Replaces redundant common features inside pods.42
Key Capabilities Details:43
Traffic Control: Load balancing, Retry / Timeout44
Observability: Latency, Success rate, Call graph45
Security: Automatically apply mTLS46
Communication: HTTP/1.1, HTTP/2, gRPC or TCP with or without mTLS47
Control Plane Interlocking: Pilot (Config data to proxies), Mixer (Policy checks, telemetry), Citadel (TLS certs to proxies)48
Proxy communication structure
Service A calls a generic service name49
iptables forces traffic to Envoy in A50
A Envoy receives B endpoint information from Istiod51
Istiod delivered via xDS API: B Service Pod List, Envoy address of each Pod, Load Balancing Policy, mTLS certificate, Routing Rules52
A Envoy selects the appropriate B Envoy (e.g., Round Robin, Least Request, Canary Routing, Version Routing)53
Flow: App A $\rightarrow$ (Kernel: iptables/eBPF intercept) $\rightarrow$ Proxy A (outbound) $\rightarrow$ (Network, mTLS) $\rightarrow$ Proxy B (inbound) $\rightarrow$ App B54
02. Disadvantages & Considerations of service mesh
Weakness / Disadvantages
Resource overhead: Add sidecar proxy to all pods, increasing CPU/Memory (approx. 10% to 30%), and needing to increase the number of nodes.55
Increased network latency: Every request goes through the proxy (App $\rightarrow$ Proxy $\rightarrow$ Network $\rightarrow$ Proxy $\rightarrow$ App), increasing latency by several ms to several tens of ms.56
Increasing Debugging Difficulty: Difficulty in separating causes when a problem occurs (Application? Network? Proxy? Mesh policy?).57
Increasing architectural complexity: Additional elements like Control Plane, Data Plane (proxy), CRD (Service Profile, etc.), and Policy (retry, timeout, etc.).58
The cost of learning: mTLS Concepts, Traffic Policy, observability, Sidecar structure.59
Introduction Considerations
Is it really necessary? Consider the number of services (10 or more), Inter-Service Call Complexity, Disability Analysis Difficulties, and required Observability.60
Retry / timeout policy design: Too aggressive $\rightarrow$ Failure amplification; Too conservative $\rightarrow$ Failure detection delay.61
Consideration of mTLS impact: Internal communication encryption $\rightarrow$ CPU usage increases, Change the authentication scheme.62
Preparation for utilization of observability: 70% of reasons for introducing mesh are due to observability (p95 latency, success rate, call graph).63
Operational automation required: Monitoring (Prometheus, Grafana), Alarm, Collect Logs.64
03. Implementing & Practicing Linkerd
Installation Order
Install WSL or Gitbash (gitbash is the simplest)65
Install Gateway API66
Install Linkerd CRD67
When installing Linkerd control plane, add --set proxyInit.runAsRoot=true68
Verified with linkerd check69
Linkerd installation script (based on docker desktop)
# Uninstall if needed
linkerd viz uninstall | kubectl delete -f -
linkerd uninstall | kubectl delete -f -
kubectl delete ns linkerd

# Install Linkerd CLI
curl -sL https://run.linkerd.io/install | sh

# Install Gateway API standard
kubectl apply --server-side -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.4.0/standard-install.yaml

# Install CRDs & Control Plane
linkerd install --crds | kubectl apply -f -
linkerd install --set proxyInit.runAsRoot=true | kubectl apply -f -

# Verify installation
linkerd check
kubectl get pods -n linkerd
kubectl get configmap -n linkerd
Linkerd Practice & Sidecar Injection
Inject Linkerd:
kubectl annotate ns default linkerd.io/inject=enabled
kubectl rollout restart deploy
Identifying Feign communication destinations: Sidecar proxy is floating, so the pod container appears to be 2/2 Running.70
pod/openfeignclient-...
pod/resttemplateserver-...
Sidecar injection targets: REST API, gRPC, Internal TCP Calls, mTLS, Traffic Control.71
Subject to careful consideration: Event Brokers/MessageQueue like Kafka, RabbitMQ (due to their own security features, clustering, and port structure).72
Check after injecting Linkerd
Verify Containers: kubectl get pods, kubectl describe pod <pod name> (app container + linkerd-proxy).73
Linkerd Viz Dashboard Commands:74
linkerd viz install | kubectl apply -f -
linkerd viz check
linkerd viz stat deploy -A
linkerd viz edges deploy -A
linkerd viz dashboard
Meaning of terminology
SR (Success Rate): Success rate.75
RPS (Requests Per Second): Requests per second (e.g., prometheus: 45.1 req/sec).76
P99: 99% of requests are processed in less than this time (e.g., mysql: 3ms, prometheus: 99ms).77
Linkerd Service Profile Configuration (YAML)
apiVersion: linkerd.io/v1alpha2
kind: ServiceProfile
metadata:
  name: payment.default.svc.cluster.local
  namespace: default
spec:
  routes:
    - name: GET /payment
      condition:
        method: GET
        pathRegex: /payment
      isRetryable: true
      timeout: 1s
      retryBudget:
        retryRatio: 0.2
        minRetriesPerSecond: 10
        ttl: 10s
timeout (1s): Failure handling when it exceeds timeout; avoids delays and mitigates bottlenecks.78
retryRatio (0.2): Allow retries up to 20% of the original request volume (e.g., 100 requests $\rightarrow$ up to 20 retries).79
minRetriesPerSecond (10): Specify at least 10 retries per second to ensure retries occur even under low traffic.80
ttl (10s): Time to Live based on requests for the last 10 seconds.80
Delete Linkerd mesh (run on gitbash)
linkerd viz uninstall | kubectl delete -f -
linkerd uninstall | kubectl delete -f -
kubectl delete crd -l linkerd.io/control-plane-ns=linkerd
kubectl delete namespace linkerd
kubectl delete namespace linkerd-viz
kubectl rollout restart deployment
04. Implementing & Practicing Istio
Installing the Istio (Run in PowerShell)
Invoke-WebRequest -Uri https://github.com/istio/istio/releases/download/1.27.3/istio-1.27.3-win.zip -OutFile istio.zip
Expand-Archive istio.zip -DestinationPath .
cd istio-1.27.3

istioctl install --set profile=demo -y
kubectl get pods -n istio-system
kubectl label namespace default istio-injection=enabled

# Install addons (Kiali, Prometheus, Grafana, Jaeger)
kubectl apply -f samples/addons
kubectl get pods -n istio-system

# Open Dashboards
istioctl dashboard kiali
istioctl dashboard grafana
istioctl dashboard prometheus

kubectl get ns --show-labels
When deleting: istioctl uninstall --purge and kubectl rollout restart deployment81
Kiali Dashboard
Purpose: Observability console to visually manage and analyze service mesh environments.82
Key Features:83
Visualize Service Mesh Topology (real-time map of call relationships).83
Check Traffic Flow (Request Rate, Success Rate, Error Rate, Response Time).84
Check the status of sidecars (Sidecar presence, Missing Sidecar detection, mTLS application).85
Verifying Settings (VirtualService, DestinationRule, Gateway, Authorization Policy).86
Security Status (mTLS enabled, encryption policy).87
Topology Components: Triangle (Service), Round Square (Workload - Deployment/ReplicaSet), Black Box (Application), Circular Icon (Unknown/External).88
Grafana Dashboard
Purpose: Integrated monitoring dashboard platform that visualizes metrics, logs, and traces.89
Key Features:
Visualize operational status: CPU, Memory, Pod Status, API Response Speed, Error Rate, DB Load, Istio Service Metrics.90
Alerting: CPU over 90%, Error surge, Pod down, Latency increase.91
Log Analysis (Loki): Search for error logs using LogQL (e.g., {app="resttemplateserver"}).92
Interlocking with Istio: Request Rate, Success Rate, P95/P99 Latency, mTLS status.93
Log Stack Build: Kubernetes + Grafana + Loki + Promtail94
Prometheus Component Comparison & Concepts
Data Source Comparison:

| Category | Prometheus UI | Grafana |

| :--- | :--- | :--- |

| PromQL test | very good | Good |

| Dashboard | restrictive | strong |

| Visualization | basics | high quality |

| Log Inquiry | impossibility | Loki interlocking |
Prometheus Concept: Time series database + monitoring engine. Periodically pulls health figures (CPU, Memory, HTTP requests, Error rate, response time) via Exporters using the Pull Method (GET /metrics).95
Sample Query: rate(container_cpu_usage_seconds_total[5m])96
Jaeger (Distributed Tracking)
Concept: System that tracks the flow of requests across multiple microservices over time.97
Core Terminology:98
Trace: One complete request flow.99
Span: Each Service Step.100
Features: Visualize Call Path, Analysis of Latency, Error tracking, Root Cause Analysis.101
Configuration (YAML):
IstioOperator (enabling Jaeger OpenTelemetry provider on port 4317).102
Telemetry mesh-default configuration for 100% random sampling.103
05. Service Mesh Gateway (G/W) & Routing Policy
Service Mesh Gateway Configuration (YAML)
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: resttemplate-gateway
  namespace: default
spec:
  selector:
    istio: ingressgateway
  servers:
    - port:
        number: 80
        name: http
        protocol: HTTP
      hosts:
        - "*"
Port Forwarding: kubectl port-forward -n istio-system svc/istio-ingressgateway 8088:80104
Virtual Service Configuration (YAML)
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: resttemplateclient-vs
  namespace: default
spec:
  hosts:
    - "*"
  gateways:
    - resttemplate-gateway
  http:
    - match:
        - uri:
            prefix: /books
      route:
        - destination:
            host: resttemplateclient-service.default.svc.cluster.local
            port:
              number: 8080
Roles: Path Routing, Version Routing, Header-based routing, Retry / Timeout, Fault Injection.105
Advanced Virtual Service Features
Resilience Configuration Settings: timeout, retries (attempts, perTryTimeout, retryOn).106
Fault Injection: Intentionally tests system resilience.107
Delay: 2 seconds fixed delay applied to 50% of requests.108
Abort: Returns HTTP status 500 error on 20% of requests.109
Deploy Canary: Progressively exposes version v2 by splitting traffic (e.g., v1 weight: 90, v2 weight: 10).110
DestinationRule Configuration (YAML)
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: resttemplateserver-dr
spec:
  host: resttemplateserver.default.svc.cluster.local
  trafficPolicy:
    loadBalancer:
      simple: ROUND_ROBIN
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        maxRequestsPerConnection: 10
    outlierDetection:
      consecutive5xxErrors: 3
      interval: 5s
      baseEjectionTime: 30s
http1MaxPendingRequests (50): Maximum number of stacked HTTP requests allowed in queue when the connection pool is full before operating a circuit breaker.111
maxRequestsPerConnection (10): Prevents connection bias and long-term connectivity issues.112
Outlier Detection Settings: Automatically excludes abnormal instances from traffic.113
consecutive5xxErrors: Number of consecutive 5xx errors allowed.114
interval: Inspection cycle.115
baseEjectionTime: Exclusion time duration.116
06. Gateway Options & Egress Traffic
Precautions when selecting G/W
Kubernetes Ingress G/W: Cannot be managed by Istio Control Plane, provides only basic L7 features, and lacks advanced routing/tracking.117
Istio G/W: Configures L4-L6 functions (Port, Host, TLS key) and binds directly to VirtualService resources for unified inside/outside mesh functions.118
Feature Scope Matrix:
K8s Ingress: Load balancing, SSL termination, Virtual hosting.119
Istio Gateway: K8s features + Advanced traffic routing, Fault injection.120
API Gateway: Traffic routing + API lifecycle management, Operation Monitoring, access authorization, key management, transformation, billing & rate limiting.121
API Gateway + Sidecar Proxy Pattern: Used at the entry point where the API gateway provides L7 capabilities and the sidecar handles Istio VirtualService routing.122
Istio Egress Traffic Control
Purpose: Controls outbound traffic from internal mesh services to external systems out of the cluster (e.g., Stripe API, External MySQL, Kafka SaaS, Google API).123
Capabilities: External Access Control, TLS Policy Audit Logs, Zero Trust enhancement, and NAT Control (Fixed Exit IP).124
Usage (outboundTrafficPolicy.mode):125
ALLOW: Allow unregistered external addresses as well.126
REGISTRY_ONLY: Allow ServiceEntry registered external domains only.127
07. Istio Removal Command
istioctl uninstall --purge -y
kubectl delete namespace istio-system
kubectl delete namespace istio-ingress
kubectl delete namespace istio-egress
kubectl label namespace default istio-injection-
kubectl rollout restart deploy