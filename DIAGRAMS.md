# Code Runner — Diagrams (draft)

Scratch file for reviewing the new Mermaid diagrams before they go into the README.
GitHub renders these blocks natively. Nothing here is committed yet.

---

## 1. High-level architecture (two planes)

Shows the split between the synchronous request plane (HTTP) and the
asynchronous execution plane (messaging), with the single MongoDB as the
shared store.

```mermaid
flowchart TB
    browser["Browser — React SPA<br/>(CodeMirror editor)"]

    subgraph req["Request plane — synchronous HTTP"]
        direction TB
        gw["Gateway<br/>Spring Cloud Gateway<br/>routes /api/**"]
        api["API Server &times;3<br/>Spring Boot<br/>create jobs · serve problems · status polls"]
    end

    subgraph exec["Execution plane — asynchronous"]
        direction TB
        ces["Code Execution Service<br/>(RabbitMQ consumer)"]
        sandbox["Sandbox container<br/>python / node<br/>capped · no net · read-only · 5s"]
    end

    mq[["RabbitMQ<br/>topic exchange<br/>job_events_exchange"]]
    mongo[("MongoDB<br/>code_execution_db<br/>problems + jobs")]

    browser -->|"POST /api/submit<br/>GET /api/status/:id"| gw
    gw -->|load-balanced| api
    api -->|"save job (PENDING)<br/>read problems"| mongo
    api -->|"publish JobCreatedEvent<br/>key: job.created"| mq

    mq -->|"worker_queue"| ces
    ces -->|"read test cases<br/>write job result"| mongo
    ces -->|"run submission"| sandbox
    sandbox -->|"stdout / stderr / exit code"| ces
    ces -->|"publish JobStatusEvent<br/>key: <serverId>"| mq
    mq -->|"server_queue.&lt;instanceId&gt;"| api

    classDef store fill:#e8f0fe,stroke:#4285f4;
    classDef broker fill:#fff3e0,stroke:#fb8c00;
    class mongo store;
    class mq broker;
```

---

## 2. Kubernetes deployment topology (the object view)

Everything lives in the `code-runner` namespace. Traefik (the k3d-bundled
ingress controller) is cluster-scoped. Shows every workload, Service,
config object, and the PVC — plus the dind-sidecar nesting.

```mermaid
flowchart TB
    user["Web browser<br/>http://code-runner.localhost/"]

    subgraph node["k3d node — host :80 to loadbalancer"]
        traefik["Traefik ingress controller<br/>(kube-system)"]

        subgraph ns["namespace: code-runner"]
            ing["Ingress: code-runner<br/>/api -> gateway-service:8080<br/>/ -> frontend:80"]

            %% --- Services ---
            svcF(["Service<br/>frontend :80"])
            svcG(["Service<br/>gateway-service :8080"])
            svcA(["Service<br/>api-server :8080<br/>= load balancer"])
            svcR(["Service<br/>rabbitmq :5672/:15672"])
            svcM(["Service<br/>mongo-headless :27017<br/>headless · replica-set DNS"])

            %% --- Workloads ---
            depF["Deployment frontend<br/>1 replica · nginx"]
            depG["Deployment gateway-service<br/>1 replica · profile=docker"]
            depA["Deployment api-server<br/>3 replicas"]
            depR["Deployment rabbitmq<br/>1 replica"]

            subgraph cesPod["Deployment code-execution-service (1 replica)"]
                direction TB
                dind["initContainer: dind<br/>docker:27-dind · privileged<br/>listens tcp://localhost:2375"]
                cesC["container: code-execution-service<br/>DOCKER_HOST=tcp://localhost:2375"]
                sbx["sandbox containers<br/>(run inside dind, invisible to k8s)"]
                cesC -->|"localhost:2375"| dind
                dind -.->|"creates"| sbx
            end

            subgraph sts["StatefulSet mongo · replica set rs0 (3 replicas, Bitnami chart)"]
                direction TB
                m0["mongo-0<br/>(primary)"]
                m1["mongo-1"]
                m2["mongo-2"]
                pvc0[("PVC data-mongo-0<br/>1Gi · RWO")]
                pvc1[("PVC data-mongo-1<br/>1Gi · RWO")]
                pvc2[("PVC data-mongo-2<br/>1Gi · RWO")]
                m0 --- pvc0
                m1 --- pvc1
                m2 --- pvc2
                m0 <-.->|replicate| m1
                m0 <-.->|replicate| m2
            end

            %% --- Config ---
            cm["ConfigMap app-config"]
            sec["Secret app-secret"]
            seed["ConfigMap mongo-seed<br/>(kustomize-generated)"]
        end
    end

    %% traffic edges
    user --> traefik --> ing
    ing -->|/| svcF --> depF
    ing -->|/api| svcG --> depG
    depG -->|"http://api-server:8080"| svcA --> depA

    depA -->|publish/consume| svcR --> depR
    depA -->|jobs + problems| svcM --> m0
    cesC -->|consume/publish| svcR
    cesC -->|test cases + results| svcM

    %% config wiring
    cm -. envFrom .-> depA
    sec -. envFrom .-> depA
    cm -. envFrom .-> depG
    cm -. envFrom .-> cesC
    sec -. envFrom .-> cesC
    sec -. secretKeyRef .-> depR
    sec -. "existingSecret (auth)" .-> sts
    seed -. "initdb on primary" .-> m0

    classDef svc fill:#e8f5e9,stroke:#43a047;
    classDef cfg fill:#f3e5f5,stroke:#8e24aa;
    classDef store fill:#e8f0fe,stroke:#4285f4;
    class svcF,svcG,svcA,svcR,svcM svc;
    class cm,sec,seed cfg;
    class pvc0,pvc1,pvc2 store;
```

---

## 3. Submission flow (sequence)

The full lifecycle of one code submission, including the per-instance
reply-queue trick and the in-memory/Mongo fallback on status polls.

```mermaid
sequenceDiagram
    autonumber
    actor C as Client
    participant G as Gateway
    participant A as API Server<br/>(instance X)
    participant DB as MongoDB
    participant MQ as RabbitMQ
    participant W as Code Exec Service
    participant S as Sandbox container

    C->>G: POST /api/submit {code, language, problemId}
    G->>A: forward (load-balanced)
    A->>DB: save Job (PENDING)
    A->>MQ: publish JobCreatedEvent<br/>key=job.created, serverId=X
    A-->>C: 202 Accepted {job_id}

    Note over MQ,W: worker_queue (shared, competing consumers)
    MQ->>W: deliver JobCreatedEvent
    W->>DB: load problem test cases
    W->>W: inject user code + test cases<br/>into language harness
    W->>S: create + start sandbox<br/>(capped, no net, ro, non-root)
    S-->>W: stdout / stderr / exit code (<=5s)
    W->>DB: upsert Job (COMPLETED / FAILED)
    W->>MQ: publish JobStatusEvent<br/>key=serverId (=X)
    MQ->>A: deliver to server_queue.X
    A->>A: store result in-memory

    loop every ~1s until done
        C->>G: GET /api/status/:id
        G->>A: forward (may hit any instance)
        alt result in this instance's memory
            A-->>C: status from in-memory store
        else miss (different instance)
            A->>DB: read Job
            A-->>C: status from MongoDB (fallback)
        end
    end
```

---

## 4. Messaging model (why one exchange, two queue kinds)

Zoom-in on the RabbitMQ routing: fan-in to the worker via a shared queue,
point-to-point reply back to the originating API server via per-instance queues.

```mermaid
flowchart LR
    a1["api-server pod 1<br/>instanceId=A"]
    a2["api-server pod 2<br/>instanceId=B"]
    a3["api-server pod 3<br/>instanceId=C"]

    subgraph mq["RabbitMQ — topic exchange: job_events_exchange"]
        direction TB
        ex{{"exchange"}}
        wq["worker_queue<br/>(durable, shared)"]
        sqa["server_queue.A"]
        sqb["server_queue.B"]
        sqc["server_queue.C"]
        ex -->|"key = job.created"| wq
        ex -->|"key = A"| sqa
        ex -->|"key = B"| sqb
        ex -->|"key = C"| sqc
    end

    w["code-execution-service<br/>4–10 competing consumers"]

    a1 -->|"publish job.created<br/>(serverId=A)"| ex
    a2 -->|"publish job.created"| ex
    a3 -->|"publish job.created"| ex

    wq --> w
    w -->|"publish result, key=serverId"| ex

    sqa --> a1
    sqb --> a2
    sqc --> a3
```

---

## 5. CI/CD — GitOps delivery

How an image gets from a git push to running in the cluster. Argo CD lives
outside the app namespace and reconciles `k8s/` from the `dev` branch.

```mermaid
flowchart TB
    dev["git push"]

    subgraph gh["GitHub"]
        repo[("repo: dev branch<br/>+ k8s/kustomization.yaml")]
        subgraph ci["GitHub Actions (ci.yml)"]
            val["validate<br/>mvn compile"]
            bld["build-images (matrix &times;4)"]
        end
    end

    hub[("DockerHub<br/>gavro081/code-runner-*")]

    subgraph cluster["k3d cluster"]
        argo["Argo CD<br/>(namespace: argocd)<br/>Application: code-runner"]
        nsapp["namespace: code-runner<br/>(Deployments, Services, StatefulSet...)"]
    end

    dev --> repo
    repo --> val --> bld
    bld -->|"v* tag: multi-arch build & PUSH"| hub
    bld -.->|"dev push: build-only, no push"| hub

    repo -->|"watches dev / k8s/"| argo
    argo -->|"kustomize apply<br/>(auto-sync: prune + self-heal)"| nsapp
    hub -->|"pull image by tag<br/>(set in kustomization images:)"| nsapp
```
