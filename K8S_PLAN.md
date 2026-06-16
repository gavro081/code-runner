# Kubernetes Plan — Code Runner (Rubric pts 5–9)

> **Purpose:** Implementation plan for the Kubernetes treatment of *Code Runner*, covering rubric
> **pts 5–9 (50%)**: Deployments + ConfigMaps/Secrets (5), Services (6), Ingress (7), a StatefulSet for the
> database (8), and everything in a dedicated **namespace** on a real cluster, **demonstrated working**
> end-to-end (9). Companion to `DEVOPS_PROJECT.md`. CD (Argo CD, the bonus) is a **separate later task** —
> the manifests here are written so Argo can sync them unchanged.
>
> **Branch:** `feature/k8s-manifests` (off `dev`). **Images:** the `0.2.0` set CI already pushed to DockerHub.

---

## 1. How the running app maps onto Kubernetes

Traced from the current code/config so the manifests match real behavior:

- **Frontend** (nginx) serves the SPA and proxies `/api` → `gateway-service:8080`.
- **Gateway** (`docker` profile, `gateway-service/.../application-docker.yml`) routes `/api/**` →
  `http://api-server:8080`. Spring Cloud LoadBalancer is **disabled** in this profile — it relies on the
  platform's DNS/round-robin.
- **api-server** (×3) talks to **MongoDB** + **RabbitMQ**; each instance owns a per-UUID RabbitMQ reply queue.
- **code-execution-service** consumes jobs from RabbitMQ and runs each submission in a throwaway sandbox
  container created via the Docker daemon it finds through `DOCKER_HOST`
  (`DefaultDockerClientConfig.createDefaultConfigBuilder()`, `CodeExecutionService.java:77`). On startup it
  builds the `python-coderunner-image` / `javascript-coderunner-image` sandbox images if missing
  (`ensureSandboxImages()`), then launches containers with `network=none`, 100 MB cap, autoremove.

### What Kubernetes replaces

| Today (Docker Compose) | In Kubernetes | Status |
|---|---|---|
| `deploy.replicas: 3` on api-server | Deployment `replicas: 3` | replaced |
| Docker DNS round-robin to `api-server` (Spring LB already off in `docker` profile) | **Service** (kube-proxy) load-balances the api-server pods | **fully replaced** |
| `/var/run/docker.sock` host mount | **DinD sidecar** (`docker:dind`, privileged); exec uses `DOCKER_HOST=tcp://localhost:2375` — **no app code change** | replaced |
| `mongo-init` bind mount for seeding | **ConfigMap** mounted at `/docker-entrypoint-initdb.d` | replaced |
| Compose env vars | **ConfigMap** (non-secret) + **Secret** (credentials) | replaced |
| `depends_on` + healthchecks | readiness/liveness **probes** (services also just retry) | replaced |
| named volume `mongo-data` | **PVC** via StatefulSet `volumeClaimTemplate` | replaced |

### Is the gateway still needed?

In k8s the gateway is a **thin `/api` pass-through** — the Ingress does L7 path routing and the api-server
Service does load balancing. **Decision: keep it** (Deployment + Service) to preserve the documented
architecture and its CORS handling; the risk/effort of removing it isn't worth it.

---

## 2. Decisions (locked)

- **Cluster: k3d** (already installed; used in labs). Sufficient — bundles **Traefik Ingress** and the
  **local-path PVC** provisioner. Supersedes the earlier minikube note in `DEVOPS_PROJECT.md`.
- **Keep the gateway** as Deployment + Service.
- **Real auth** on Mongo + RabbitMQ; credentials in **Secrets**, non-secret config in a **ConfigMap**.
- **RabbitMQ = Deployment** (not StatefulSet — pt 8's StatefulSet requirement is met by MongoDB).
- **Images** pulled from DockerHub at tag **`0.2.0`** (public; `imagePullPolicy: IfNotPresent`).
- **No app code changes**; no new images.

---

## 3. Prerequisites (one-time)

`k3d`, `kubectl`, `docker` are already installed; no cluster exists yet. Create one that publishes Traefik on
localhost so the Ingress is reachable:

```bash
k3d cluster create code-runner -p "80:80@loadbalancer" -p "443:443@loadbalancer" --agents 1
echo "127.0.0.1 code-runner.localhost" | sudo tee -a /etc/hosts   # so the Ingress host resolves
```

> **Highest-risk item — validate FIRST.** The exec service needs a **privileged `docker:dind` sidecar**, and
> k3d nodes are themselves containers, so this is nested Docker-in-Docker. It generally works (k3d runs nodes
> privileged), but confirm it early. Fallbacks if it won't run: recreate the k3d node with extra privileges,
> or (last resort) a VM-backed minikube just for that pod.

---

## 4. Manifest set — `k8s/` directory

One file per concern; a `kustomization.yaml` sets the namespace and applies everything (`kubectl apply -k k8s/`)
and is the future Argo CD entrypoint.

| File | Objects | Notes |
|---|---|---|
| `namespace.yaml` | Namespace `code-runner` | pt 9 |
| `config.yaml` | ConfigMap `app-config` | non-secret env (§5) |
| `secret.example.yaml` | Secret `app-secret` (template) | committed; real `secret.yaml` **gitignored** (mirrors the `application.properties` pattern) |
| `mongo.yaml` | **StatefulSet** `mongo` + headless Service + `volumeClaimTemplate` (PVC) | pt 8; seeded via ConfigMap at `/docker-entrypoint-initdb.d` |
| `rabbitmq.yaml` | Deployment + Service (5672; optional 15672) | creds from Secret |
| `api-server.yaml` | Deployment **replicas: 3** + Service `api-server:8080` | the Service **is** the load balancer (pt 6) |
| `gateway.yaml` | Deployment + Service `gateway-service:8080` | env `SPRING_PROFILES_ACTIVE=docker` |
| `code-execution.yaml` | Deployment: exec container **+ DinD native sidecar** | the hard part (§6); no Service (RabbitMQ consumer) |
| `frontend.yaml` | Deployment + Service `frontend:80` | |
| `ingress.yaml` | Ingress (Traefik), host `code-runner.localhost` | pt 7 |
| `kustomization.yaml` | `namespace: code-runner`; `configMapGenerator` for the Mongo seed from `mongo-init/seed.js` | apply entrypoint |

---

## 5. Config & Secrets (pt 5)

**ConfigMap `app-config`** (non-secret):
`SPRING_DATA_MONGODB_DATABASE=code_execution_db`, `SPRING_RABBITMQ_HOST=rabbitmq`,
`SPRING_RABBITMQ_PORT=5672`, `SERVER_PORT=8080`.

**Secret `app-secret`** (real credentials):
- `SPRING_DATA_MONGODB_URI=mongodb://<user>:<pass>@mongo:27017/code_execution_db?authSource=admin`
- `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`
- infra-pod keys: `MONGO_INITDB_ROOT_USERNAME/PASSWORD`, `RABBITMQ_DEFAULT_USER/PASS`

Spring apps (api-server, gateway, exec) consume via `envFrom: [configMapRef: app-config, secretRef: app-secret]`.
The mongo and rabbitmq pods map **only their own keys** via `valueFrom.secretKeyRef` (so broker/DB pods don't
inherit the Spring URIs). Real `secret.yaml` is gitignored; `secret.example.yaml` is committed with placeholder
values.

### MongoDB seeding + auth
- `mongo` StatefulSet sets `MONGO_INITDB_ROOT_USERNAME/PASSWORD` → root user in the `admin` db (hence
  `authSource=admin` in the URI).
- The seed ConfigMap is generated from the existing `mongo-init/seed.js` (kustomize `configMapGenerator`) and
  mounted at `/docker-entrypoint-initdb.d`; it runs once on first init (empty PVC), authenticated as root —
  same mechanism as Compose, just sourced from a ConfigMap instead of a bind mount.

---

## 6. code-execution-service + DinD (the hard part)

- **dind native sidecar:** `docker:dind` declared as an `initContainer` with `restartPolicy: Always` (a k8s
  **native sidecar** — guaranteed to start *before* the main container), `securityContext.privileged: true`,
  `env DOCKER_TLS_CERTDIR=""` (daemon listens on `tcp://0.0.0.0:2375`), an `emptyDir` at `/var/lib/docker`,
  and a `readinessProbe` on port 2375.
- **exec container:** image `gavro081/code-runner-code-execution-service:0.2.0`,
  `env DOCKER_HOST=tcp://localhost:2375`, `envFrom` ConfigMap + Secret. On startup `ensureSandboxImages()`
  builds the python/js images against the dind daemon, then runs sandbox containers **inside** dind.
- **Ordering matters:** `ensureSandboxImages()` does **not** retry on failure (`CodeExecutionService.java:100`),
  so dind must be ready before the exec container starts. The native-sidecar `restartPolicy: Always` + the
  2375 readiness probe guarantee this. Backup if needed: a small `wait-for-docker` regular initContainer.

---

## 7. Ingress (pt 7) — Traefik on k3d

- Host `code-runner.localhost`, `ingressClassName: traefik`.
- `/api` → `gateway-service:8080` — **no path rewrite** (gateway predicate is `Path=/api/**`; api-server
  expects `/api/...`).
- `/` → `frontend:80`.
- The frontend nginx's own `/api` proxy becomes redundant for external traffic but is left untouched
  (harmless; keeps the frontend image standalone) → **no frontend change**.

## 8. Probes (kept simple)

`tcpSocket` readiness on each Spring service (8080) and on mongo (27017) / rabbitmq (5672) to avoid assuming
actuator is present; `httpGet /` on the frontend; dind readiness on 2375.

---

## 9. Out of scope (separate later tasks)

- **Argo CD / CD bonus** — manifests are Argo-ready (kustomize) but no Argo install/config here.
- No app code changes; no new images.

## 10. Verification (end-to-end, pt 9)

1. **DinD first (de-risk):** apply namespace + config + `code-execution.yaml`; confirm the dind sidecar is
   `Running` and the exec container logs `Built sandbox image 'python-coderunner-image'` / `javascript-...`.
2. **Full stack:** `kubectl apply -k k8s/`; `kubectl -n code-runner get pods` all `Running`/`Ready`;
   `get statefulset,svc,ingress` present.
3. **Seed check:** exec a (authenticated) mongo shell → `problems` collection has 7 docs.
4. **App works:** browse `http://code-runner.localhost/`, submit a **Python** and a **JavaScript** solution →
   both return `PASSED ALL TEST CASES` (same check used for Compose).
5. **Load balancing:** `kubectl -n code-runner get endpoints api-server` lists 3 pod IPs.
6. Update `DEVOPS_PROJECT.md` (flip pts 5–9, switch cluster decision minikube→k3d, add a progress-log entry);
   capture `kubectl get` output + a submission screenshot for the report.

---

## 11. Execution order

1. Create the k3d cluster (§3) and **validate DinD** with a throwaway exec pod.
2. Write `k8s/` manifests: namespace → config/secret → mongo → rabbitmq → api-server → gateway →
   code-execution → frontend → ingress → kustomization.
3. `kubectl apply -k k8s/`, work through the verification checklist (§10).
4. Update `DEVOPS_PROJECT.md`; commit on `feature/k8s-manifests` (commits are manual).
