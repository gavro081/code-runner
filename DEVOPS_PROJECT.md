# DevOps Project — Code Runner

> **Purpose of this file:** This is the single source of truth for the DevOps course project built on top of the
> existing *Code Runner* application. It captures what the app is, how it's wired, the grading rubric, the technical
> decisions, and the running progress. Paste/point a fresh session at this file to get fully up to speed.
>
> **Last updated:** 2026-06-16 (after DB consolidation: dropped PostgreSQL, now single MongoDB)

---

## 1. The task (what's being graded)

The assignment (originally in Macedonian) is to take a ready-made application with **at least three services
including a database**, and produce a full DevOps treatment of it. Code Runner qualifies easily (frontend +
multiple backends + 2 databases + message broker).

### Grading rubric — 100% total

| #  | Weight | Requirement | Status |
|----|--------|-------------|--------|
| 1  | 10%    | App on a **public git repository** | ✅ already done — https://github.com/gavro081/code-runner |
| 2  | 10%    | **Dockerize** the application | ✅ done — multi-stage Dockerfiles for all services + frontend (nginx) |
| 3  | 10%    | **Orchestrate** app + database with **Docker Compose** | ✅ done — full stack in `docker-compose.yml`, end-to-end submit verified |
| 4  | 20%    | Choose a **CI platform** (GitHub Actions / GitLab CI / Jenkins…) and set up a pipeline: on `git push`, build the new Docker image version and push it to a **registry** (e.g. DockerHub). **Bonus:** add a CD stage that deploys to a real environment (server / cloud / Kubernetes / Argo CD). | ⬜ not started |
| 5  | 10%    | Kubernetes **Deployment** for the app + needed **ConfigMaps/Secrets** | ⬜ not started |
| 6  | 10%    | Kubernetes **Service** for the app | ⬜ not started |
| 7  | 10%    | Kubernetes **Ingress** for the app | ⬜ not started |
| 8  | 10%    | Kubernetes **StatefulSet** for the database (now a single **MongoDB**) + needed ConfigMaps/Secrets | ⬜ not started |
| 9  | 10%    | Put all manifests in a **separate namespace** on a cluster and **demonstrate it works** | ⬜ not started |

**Goal: complete every subtask for full marks, including the CD bonus if feasible.**

---

## 2. Application overview

**Code Runner** is a LeetCode-style platform: users solve coding problems in the browser, submit code, and it runs
in **sandboxed Docker containers** on the backend with strict resource limits, returning pass/fail + stdout/stderr.

### Architecture (current / local)

```
Browser (React, :5173)
   │  HTTP  http://localhost:8080/api/**   (hardcoded in frontend — see §5)
   ▼
Gateway (Spring Cloud Gateway, :8080)
   │  round-robin load balance
   ▼
API Server  ×3  (:8081, :8082, :8083)  ── Mongo ─► MongoDB (:27017)   [problems + jobs]
   │  publish JobCreatedEvent
   ▼
RabbitMQ (:5672, mgmt :15672)
   ▼
Code Execution Service (:8084)
   │  reads problem/testcases from MongoDB
   │  spins up a throwaway Docker container per submission  ◄── ⚠️ talks to the Docker daemon
   ▼
python:3.10-slim / node:18-slim sandbox (mem/cpu/pids capped, no net, ro fs, non-root, 5s timeout)
   │  stdout/stderr + exit code
   ▼  JobResultEvent routed back to the originating API Server instance (per-instance reply queue)
MongoDB  (job marked COMPLETED/FAILED in the `jobs` collection)
```

> **DB consolidation (done 2026-06-16):** Jobs were migrated off PostgreSQL/JPA into MongoDB. There is now a
> **single database** — MongoDB `code_execution_db` holds both the `problems` and `jobs` collections. Postgres,
> the JDBC driver, and the JPA starter were removed entirely. See §5.4 and the progress log.

### Flow
1. Client `POST /api/submit` (code, language, problemId).
2. API Server saves a `PENDING` job to MongoDB, publishes `JobCreatedEvent` to RabbitMQ, returns `202` + `job_id`.
3. Client polls `GET /api/status/{job_id}` every second (served from an in-memory store first, falling back to Mongo).
4. Code Execution Service consumes the event, loads the problem's test cases from Mongo, injects user code + test
   cases into a language harness template, and runs the composed script in a sandboxed container.
5. Result persisted to MongoDB; `JobResultEvent` routed back to the originating API Server instance via its
   per-instance RabbitMQ reply queue.
6. Next client poll returns the finished result.

The execution service auto-scales RabbitMQ consumers (4→10 threads) and pauses consumption when system CPU > 85%.

---

## 3. Services inventory

| Service | Tech | Build | Port(s) | Depends on | Notes |
|---------|------|-------|---------|-----------|-------|
| **frontend** | React 19 + TS + Vite + Tailwind + CodeMirror | `npm`, Vite | 5173 (dev) | Gateway | Backend URL **hardcoded** to `http://localhost:8080` (§5). Not yet containerized. |
| **gateway-service** | Spring Cloud Gateway (WebFlux) | Maven, Java 17 | 8080 | api-server | Routes `/api/**` → `lb://api-server`. Uses a **static instance list** `localhost:8081-8083` (§5). Standalone module (not in root pom). |
| **api-server** | Spring Boot (web, Mongo, AMQP) | Maven, Java 17 | 8081–8083 (3 instances) | Mongo, RabbitMQ | Module of root pom. Each instance gets a random `instanceId` (UUID) for its reply queue. |
| **code-execution-service** | Spring Boot + **docker-java** SDK | Maven, Java 17 | 8084 | RabbitMQ, Mongo, **Docker daemon** | ⚠️ The hard part: it launches containers via the Docker daemon (`DefaultDockerClientConfig` → `DOCKER_HOST`/socket). NOT a module of root pom. Mongo starter comes transitively via `common`. |
| **common** | Shared DTOs / Mongo `@Document` models / events / RabbitMQ constants | Maven, Java 17 | — | — | Library, `spring-boot-maven-plugin` skipped. Module of root pom. |
| **RabbitMQ** | `rabbitmq:3-management` | image | 5672 / 15672 | — | guest/guest. Already in `docker-compose.yml`. |
| **MongoDB** | external (local install today) | image (planned) | 27017 | — | **The single database.** DB `code_execution_db`, holds `problems` + `jobs` collections. Problems seeded via `commands/seed-db.sh` (`mongoimport`); jobs created at runtime. |

### Maven module layout (important quirk)
- Root `pom.xml` (`packaging=pom`, parent spring-boot 3.5.6, `java.version=17`) declares only **`api-server`** and **`common`** as modules.
- **`gateway-service`** and **`code-execution-service`** are **standalone** Maven projects with their *own* Spring Boot parent (3.4.1 and inherited respectively) — they are NOT reactor modules. Each has its own `mvnw`.
- **All five poms now target `java.version=17`** (synced 2026-06-16). Local toolchain is **Java 17** + Maven 3.9.11 → use a Java 17 base image for all Docker builds.
- `code-execution-service` depends on `common:0.0.1-SNAPSHOT` from the local Maven repo, so `common` must be `mvn install`ed first. Same for `api-server`.

### Config files
- `api-server` & `code-execution-service`: `src/main/resources/application.properties` (gitignored) + `application-example.properties` (committed). Now contain only RabbitMQ + Mongo settings — all `localhost`. (Postgres/JPA lines removed.)
- `gateway-service`: `application.yml` with the static instance list.
- `.gitignore` excludes `**/application.properties`, `task.txt`, `about/`, `.venv/`, `.idea`.

---

## 4. How it's run today (local)
- `start.sh`: frees ports, `docker-compose up` (RabbitMQ only), then `mvn spring-boot:run` for gateway, 3× api-server (via `--server.port`), exec service, then `npm run dev` for frontend. Requires local MongoDB (Postgres no longer needed).
- `commands/`: `seed-db.sh` (mongoimport problems), `check-ports.sh`, load-test scripts (`command*.sh`/`test*.sh`, need `hey`/`ab`).
- `code_execution_db.problems.json` (in `commands/`) is the seed dataset.

---

## 5. Key technical challenges & decisions to make

These are the things that will actually take thought during dockerization / k8s. Capture decisions here as we make them.

### 5.1 ⚠️ Docker-in-Docker for the execution service (THE big one)
`code-execution-service` creates sandbox containers via the host Docker daemon. Implications:
- **Docker Compose:** mount `/var/run/docker.sock` into the container. The sandbox images (`python-coderunner-image`, `javascript-coderunner-image`) must be **built and present on the host** beforehand — the service references them by name, it does not build them.
- **Kubernetes:** no Docker daemon by default (containerd). Options to evaluate:
  1. **DinD sidecar** (`docker:dind`) next to the exec service, sharing a network/socket — most faithful, needs `privileged`.
  2. Mount the node's container runtime socket — brittle / not containerd-compatible with docker-java.
  3. Swap the sandbox mechanism (e.g. a separate runner pod / Job per submission, or gVisor/Kata) — biggest code change.
  - **Leaning toward DinD sidecar** for the demo to minimize app code changes. Decide & record here.
- The sandbox image names (`python-coderunner-image`, `javascript-coderunner-image`) are hardcoded in `CodeExecutionService.init()`. Whatever environment runs the exec service must have these images built (`Dockerfile.python` / `Dockerfile.javascript` under `code-execution-service/src/main/resources/docker/`).

### 5.2 Gateway service discovery
- Current gateway uses a **static** `spring.cloud.discovery.client.simple` list of `localhost:8081-8083`.
- In Compose/K8s there's no fixed instance list. Approaches:
  - **Compose:** scale `api-server` (`deploy.replicas: 3`) and route to `http://api-server:8080`; Docker's internal DNS round-robins.
  - **K8s:** a `Service` in front of the api-server `Deployment` gives a stable DNS name + load balancing; gateway routes to `http://api-server:8080`.
- → The per-instance UUID reply-queue design still works fine behind a Service (each pod still has its own queue).

### 5.3 Frontend → backend URL is hardcoded
- `frontend/src/components/*.tsx` hardcode `http://localhost:8080/api/...` (4–5 call sites in `CodeIde.tsx`, plus `AddProblem.tsx`, `ProblemsList.tsx`).
- Must become configurable: a Vite env var (`VITE_API_BASE_URL`) injected at build time, or serve frontend behind the same Ingress host and use a relative `/api` path. → Decide; relative `/api` via Ingress is cleanest for the k8s demo.
- Frontend needs a production build + static serving (nginx) Dockerfile (none exists yet).

### 5.4 Database as a container (single MongoDB)
- After the DB consolidation there is **one** database: MongoDB `code_execution_db` with `problems` + `jobs` collections. Today it's external/local; for Compose & K8s it becomes a container.
- Mongo needs seeding with `code_execution_db.problems.json` (init container / one-shot Job / `docker-entrypoint-initdb.d`). The `jobs` collection is created automatically at runtime.
- Maps cleanly onto the rubric: **one StatefulSet for MongoDB** (with a PVC) satisfies pt 8. RabbitMQ can be a separate StatefulSet or a plain Deployment. (No more Postgres StatefulSet to worry about — this is the payoff of the consolidation.)

### 5.5 Java version & image base
- ✅ JDK standardized on **17** across all five poms (done 2026-06-16). Use a Java 17 base image (`eclipse-temurin:17-jdk` for the build stage, `-jre-alpine` for runtime) with **multi-stage builds** (Maven build stage → slim JRE runtime) so we don't depend on a pre-built `target/*.jar`.
- Remember the standalone-module quirk: build `common` first (install to local repo), or restructure so all four are reactor modules / build each independently in its own Dockerfile.

### 5.6 Secrets
- Connection settings currently live in gitignored `application.properties`. In Docker/K8s, pass via **env vars** (Spring relaxed binding: `SPRING_DATA_MONGODB_URI`, `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`, etc.) sourced from Compose env / K8s ConfigMaps + Secrets. No need to bake creds into images. (No more Postgres/JDBC creds after the consolidation.)

---

## 6. Existing assets & prior attempts

- Nothing worth noting

---

## 7. Planned roadmap (mapped to rubric)

Order chosen so each step unblocks the next. Each gets its own feature branch off `dev` (see §8).

1. **Dockerize (pt 2)** — multi-stage Dockerfiles for `gateway`, `api-server`, `code-execution-service`, `frontend` (nginx). Make frontend API URL configurable. Externalize config to env vars. Keep building the python/js sandbox images.
2. **Docker Compose (pt 3)** — full stack: frontend, gateway, api-server ×3, exec service (+docker.sock), rabbitmq, mongo (+seed). End-to-end submit works via compose.
3. **CI (pt 4)** — GitHub Actions: on push to `dev`/`main`, build + push each service image to DockerHub with tags. **Bonus CD:** deploy to the cluster (kubectl apply / Argo CD) at the end.
4. **Kubernetes manifests (pts 5–8)** — namespace; Deployments + Services for frontend/gateway/api-server/exec; ConfigMaps + Secrets; Ingress (single host, `/` → frontend, `/api` → gateway); StatefulSet for MongoDB (+ RabbitMQ). Solve the DinD question for the exec service.
5. **Cluster demo (pt 9)** — install minikube/kind, deploy everything into the namespace, enable ingress addon, demonstrate a working submission end-to-end. Capture screenshots/commands for the report.

### Decisions (locked 2026-06-16)
- [x] CI registry: **DockerHub** (matches the task's example; needs account + access token in repo secrets).
- [x] CI platform: **GitHub Actions** (repo is on GitHub).
- [x] Local cluster: **minikube** (built-in ingress addon; needs install — not present yet).
- [x] Exec-service sandbox in k8s: **DinD sidecar** (`docker:dind`, privileged) — minimal app code change.
- [x] Pursue the **CD bonus**: **YES** — pipeline ends by deploying to the cluster.

### Still open
- [x] Frontend API URL strategy: **relative `/api`** (resolved). Frontend calls `${API_BASE_URL}/api/...`
  where `API_BASE_URL` defaults to `""` (relative). In Compose, nginx proxies `/api` → gateway; in dev,
  the Vite proxy does the same. Build-time `VITE_API_BASE_URL` arg remains available as an override.
- [x] Standard **JDK version**: **17** across all poms (synced 2026-06-16).
- [ ] CD mechanism for the bonus: plain `kubectl apply` from Actions vs **Argo CD** (GitOps). Decide at pt 4.

---

## 8. Git workflow

- **`dev`** is the integration baseline. All new work branches **off `dev`**.
- One **feature branch per piece of work**, e.g. `feature/dockerize`, `feature/docker-compose`,
  `feature/ci-pipeline`, `feature/k8s-manifests`, `feature/cluster-demo`. Merge each back into `dev` when done.
- **`main`** is touched only at the **very end**: merge `dev` → `main` once everything works.
- `docker-attempt`, `mongodb-integration`, `old-impl` are pre-existing branches — reference only, not part of this workflow.
- Repo is already public on GitHub (rubric pt 1 ✅).
- Don't commit anything automatically. All commits will be done manually.

---

## 9. Progress log

Append dated entries as work happens so a future session sees exactly where things stand.

- **2026-06-16** — Read the whole repo + task. Created this context doc on `dev`. No DevOps artifacts built yet.
  Confirmed: public repo exists (pt 1 done); only RabbitMQ is in the current `docker-compose.yml`; a diverged
  `docker-attempt` branch has reusable Compose/Dockerfile patterns.
- **2026-06-16** — **DB consolidation** on branch `feature/migrate-db`: migrated `Job` from PostgreSQL/JPA to
  MongoDB so the app has a single database. Changes: `Job.java` → `@Document(collection="jobs")` with Spring Data
  Mongo annotations (dropped Hibernate `@CreationTimestamp`; `createdAt` still set in code); `JobRepository` &
  `JobHandlerRepository` → `MongoRepository`; removed `@Transactional` from `JobHandlerService` (single-doc op);
  removed stray `jakarta.persistence.Column` from `Problem.java`; deleted `CommonJpaAutoConfiguration` + its
  `AutoConfiguration.imports`; removed `spring-boot-starter-data-jpa` + `postgresql` from the 3 poms; stripped
  `spring.jpa.*`/`spring.datasource.*` from both services' properties. Reactor + exec-service both build clean;
  zero residual JPA/Postgres references. Next up: pt 2 (dockerize) on a fresh feature branch off `dev`.
  - *Gotcha hit & fixed:* the first `mvn install` (no `clean`) left a stale compiled `AutoConfiguration.imports` in
    `common/target` → it got packaged into the `.m2` `common` jar and broke `start.sh` with
    `Unable to read meta-data for ... CommonJpaAutoConfiguration`. Fix: `mvn clean install`. Lesson: always `clean`
    after deleting source files (matters for the Docker build stage too).
- **2026-06-16** — Synced **`java.version` to 17** across all five poms (only the root pom was still on 21; modules
  already overrode to 17). Reactor + exec-service rebuild clean. Locks in a Java 17 base image for all Docker builds.
- **2026-06-16** — **Dockerize + Compose (pts 2 & 3)** on branch `feat/dockerization`. Built and verified the full
  stack end-to-end (Python *and* JavaScript submissions return `PASSED ALL TEST CASES`). Details:
  - **Multi-stage Dockerfiles** (`maven:3.9-eclipse-temurin-17` build → `eclipse-temurin:17-jre-jammy` runtime;
    **not** `-jre-alpine` — that tag has no arm64 manifest, build failed on Apple Silicon). `api-server` &
    `code-execution-service` build from the **repo root context** (they need the `common` module + root parent pom):
    each Dockerfile installs `common` to the local repo first, then packages the service. `gateway-service` &
    `frontend` build from their own dirs. `.dockerignore` at root + per service.
  - **Code execution kept working inside a container**: `code-execution-service` mounts `/var/run/docker.sock` and
    runs as root, so its docker-java client talks to the **host daemon** and launches sandbox containers as siblings
    (network none, autoremove, capped — unchanged). Runtime image needs **no docker CLI** (SDK uses the JDK's native
    unix-socket support).
  - **Auto-build of sandbox images**: added `ensureSandboxImages()` in `CodeExecutionService.init()` — on startup it
    `inspectImage`s `python-coderunner-image` / `javascript-coderunner-image` and, if missing, **builds them via
    docker-java** (`buildImageCmd` + `withPull(true)`) from the `classpath:docker/Dockerfile.*` resources. This
    removes the manual `docker build` step entirely; verified both images get created on the host daemon at startup.
  - **Gateway discovery**: new `application-docker.yml` (profile `docker`, set in Compose) drops the static
    `localhost:8081-8083` list and routes `/api/**` directly to `http://api-server:8080`; Compose runs `api-server`
    with `deploy.replicas: 3` and Docker DNS round-robins. Per-instance reply queues still work (status polls that
    hit a different replica fall back to Mongo).
  - **Frontend**: nginx image serving the Vite prod build; hardcoded `http://localhost:8080` replaced with
    `${API_BASE_URL}/api/...` (`frontend/src/utils/config.ts`, default `""`). nginx proxies `/api` → gateway; Vite
    dev proxy added so `npm run dev` still works with the same relative paths.
  - **MongoDB**: now a container with a named volume; `mongo-init/seed.js` (generated from
    `commands/code_execution_db.problems.json`) seeds the 7 problems on first init via `docker-entrypoint-initdb.d`.
    Host port mapped to **27018** to avoid clashing with a local `mongod` on 27017 (internal services use
    `mongo:27017`).
  - **Config externalized to env vars** in Compose (`SPRING_DATA_MONGODB_URI`, `SPRING_RABBITMQ_*`, `SERVER_PORT`),
    overriding the packaged `application.properties`. No creds baked into images.
  - **`start.sh` is now superseded** by `docker compose up` (it would both `compose up` the full stack and re-run the
    services via Maven — don't use it anymore). Left in place for reference; safe to delete later.

---

## 10. Quick reference

- Repo: https://github.com/gavro081/code-runner — branch `main` (public), working baseline `dev`.
- Local run: `./start.sh` (needs local MongoDB); seed Mongo with `./commands/seed-db.sh`; app at http://localhost:5173.
- Ports: 5173 frontend · 8080 gateway · 8081-8083 api · 8084 exec · 5672/15672 rabbitmq · 27017 mongo (Postgres removed).
- Sandbox images the exec service expects by name: `python-coderunner-image`, `javascript-coderunner-image`
  (built from `code-execution-service/src/main/resources/docker/Dockerfile.{python,javascript}`).
