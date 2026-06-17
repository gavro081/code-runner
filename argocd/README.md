# Argo CD

GitOps continuous deployment for Code Runner. Argo CD watches the `dev` branch and keeps the
`code-runner` namespace in sync with the manifests under [`../k8s`](../k8s).

## One-time setup

```bash
# 1. install Argo CD into the cluster
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml --server-side

# 2. bootstrap the app secret (gitignored - not GitOps-managed; see k8s/kustomization.yaml)
kubectl apply -n code-runner -f ../k8s/secret.yaml # copy from k8s/secret.example.yaml first

# 3. register the Application
kubectl apply -f application.yaml
```

After this, Argo continuously deploys: push a manifest change to `dev` (e.g. bump the image tag in
`k8s/kustomization.yaml`) and Argo auto-syncs it — no `kubectl apply` needed.

## Accessing the UI

```bash
kubectl -n argocd port-forward svc/argocd-server 8081:443
# open https://localhost:8081  (accept the self-signed cert)
# user: admin
# pass: kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d
```

## Status from the CLI

```bash
kubectl -n argocd get application code-runner # SYNC STATUS / HEALTH
```
