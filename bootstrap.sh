#!/usr/bin/env bash
#
# bootstrap.sh — stand up the whole Code Runner stack from scratch on a local k3d cluster,
# deployed via Argo CD (GitOps). Re-runnable: it deletes and recreates the cluster.
#
# What it does:
#   1. (re)create a k3d cluster with Traefik published on localhost :80/:443
#   2. create the `code-runner` namespace + apply the bootstrap Secret (gitignored, not GitOps-managed)
#   3. install Argo CD
#   4. wait for Argo CD to be ready
#   5. register the Argo CD Application -> Argo deploys the app from the `dev` branch (k8s/)
#   6. wait for the app to be Synced + Healthy, then print access info
#
# Prereqs: k3d, kubectl, docker, internet. Run from the repo root: ./bootstrap.sh
#
# NOTE: Argo deploys whatever is on the `dev` branch on GitHub (true GitOps) — not your local
# working tree. Commit/push manifest changes to `dev` for them to take effect.

set -euo pipefail

CLUSTER="code-runner"
NS="code-runner"
ARGO_NS="argocd"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# preflight: the bootstrap secret must exist and have real creds
if [ ! -f "$HERE/k8s/secret.yaml" ]; then
  echo "!! k8s/secret.yaml not found — creating it from the template."
  cp "$HERE/k8s/secret.example.yaml" "$HERE/k8s/secret.yaml"
  echo "!! Fill in real credentials in k8s/secret.yaml, then re-run ./bootstrap.sh"
  exit 1
fi
if grep -qE "<MONGO_PASS>|<RABBIT_PASS>|<REPLICA_SET_KEY>" "$HERE/k8s/secret.yaml"; then
  echo "!! k8s/secret.yaml still has placeholder values — fill in real credentials and re-run."
  exit 1
fi

echo "==> 1/6  (re)create k3d cluster '$CLUSTER' (Traefik on :80/:443)"
k3d cluster delete "$CLUSTER" >/dev/null 2>&1 || true
k3d cluster create "$CLUSTER" -p "80:80@loadbalancer" -p "443:443@loadbalancer" --agents 1

echo "==> 2/6  namespace + bootstrap secret (out-of-band; Argo does not manage it)"
kubectl create namespace "$NS" --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n "$NS" -f "$HERE/k8s/secret.yaml"

echo "==> 3/6  install Argo CD (server-side apply for the large CRDs)"
kubectl create namespace "$ARGO_NS" --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n "$ARGO_NS" --server-side --force-conflicts \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml >/dev/null

echo "==> 3.5  enable Helm support in Argo CD's kustomize build (Bitnami MongoDB chart)"
# k8s/kustomization.yaml inflates the Bitnami chart via `helmCharts`, which kustomize only
# allows with --enable-helm. Argo CD reads this from the argocd-cm ConfigMap; restart the
# repo-server so it picks up the change (the rollout wait below covers the restart).
kubectl -n "$ARGO_NS" patch configmap argocd-cm --type merge \
  -p '{"data":{"kustomize.buildOptions":"--enable-helm"}}'
kubectl -n "$ARGO_NS" rollout restart deploy/argocd-repo-server

echo "==> 4/6  wait for Argo CD to be ready"
kubectl -n "$ARGO_NS" rollout status deploy/argocd-repo-server --timeout=300s
kubectl -n "$ARGO_NS" rollout status deploy/argocd-server      --timeout=300s
kubectl -n "$ARGO_NS" rollout status statefulset/argocd-application-controller --timeout=300s

echo "==> 5/6  register the Argo CD Application (GitOps deploys the app from dev)"
kubectl apply -f "$HERE/argocd/application.yaml"

echo "==> 6/6  wait for the app to sync (Argo pulls k8s/ from the dev branch)"
for i in $(seq 1 50); do
  sync=$(kubectl -n "$ARGO_NS"  get application code-runner -o jsonpath='{.status.sync.status}'   2>/dev/null || true)
  health=$(kubectl -n "$ARGO_NS" get application code-runner -o jsonpath='{.status.health.status}' 2>/dev/null || true)
  printf '    [%02d] sync=%s health=%s\n' "$i" "${sync:-?}" "${health:-?}"
  [ "$sync" = "Synced" ] && [ "$health" = "Healthy" ] && break
  sleep 6
done

echo
echo "================================================================"
echo "  Done."
echo "  App:      http://code-runner.localhost/"
echo "  Argo UI:  kubectl -n argocd port-forward svc/argocd-server 8081:443"
echo "            -> https://localhost:8081   (user: admin)"
echo "            pass: $(kubectl -n "$ARGO_NS" get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' 2>/dev/null | base64 -d || echo '<run again once Argo is up>')"
echo "================================================================"
