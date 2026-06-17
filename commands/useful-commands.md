# useful commands

quick reference for commands i need all the time for interacting with the code-runner cluster, Argo CD, images, and MongoDB.

## cluster (k3d)

```sh
k3d cluster list                  # list clusters
k3d cluster stop code-runner      # stop the cluster (keeps state)
k3d cluster start code-runner     # start it back up
k3d cluster delete code-runner    # delete it entirely
```

## kubectl basics

```sh
kubectl -n code-runner get all # everything in the code-runner namespace
kubectl -n code-runner get pods -w # watch pods as they change state
```

```sh
open http://code-runner.localhost/ # open app frontend
```

## port-forwarding

```sh
# RabbitMQ management UI
kubectl -n code-runner port-forward svc/rabbitmq 15672:15672
open http://localhost:15672

# Argo CD server (accept the self-signed cert)
kubectl -n argocd port-forward svc/argocd-server 8081:443
open https://localhost:8081 # credentials below
```

## Argo CD

```sh
# watch the code-runner application reconcile
kubectl -n argocd get application code-runner -w
```

CLI login:

```sh
# port-forward first if Argo CD runs inside the cluster
kubectl port-forward svc/argocd-server -n argocd 8080:443
argocd login localhost:8080 #
# default username is admin
# fetch the admin password
kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath='{.data.password}' | base64 -d
```

## images and versioning

```sh
# bump the image tag across the kustomization
sed -i '' 's/newTag: "1.0.0"/newTag: "1.1.0"/g' k8s/kustomization.yaml

# set all service images at once
cd k8s
kustomize edit set image \
  gavro081/code-runner-api-server:1.1.0 \
  gavro081/code-runner-gateway-service:1.1.0 \
  gavro081/code-runner-code-execution-service:1.1.0 \
  gavro081/code-runner-frontend:1.1.0
```

inspect which image each pod is actually running:

```sh
# just the code-runner pods, deduped
kubectl -n code-runner get pods \
  -o jsonpath='{range .items[*]}{.spec.containers[*].image}{"\n"}{end}' \
  | grep -o 'code-runner-[a-z-]*:[0-9.]*' | sort -u

# every pod in the cluster, with namespace and name
kubectl get pods -A \
  -o jsonpath='{range .items[*]}{.metadata.namespace}{"\t"}{.metadata.name}{"\t"}{range .spec.containers[*]}{.image}{"\n"}{end}{end}'
```

## git tags

```sh
git tag -a vx.y.z -m "tag message goes here"
git push origin tag <tag_name>
```

## CI (GitHub Actions)

```sh
gh run list --limit 3 # last 3 runs
gh run watch <run-id> # specific run (get id from above command)
```

## MongoDB


```sh
# port-forward the primary pod
kubectl -n code-runner port-forward pod/mongo-0 27020:27017
```

connection string:

```
mongodb://root:coderunner-mongo-pw@localhost:27020/?authSource=admin&directConnection=true
```

grab the creds from the secret (in another terminal):

```sh
kubectl -n code-runner get secret app-secret -o jsonpath='{.data.MONGO_INITDB_ROOT_USERNAME}' | base64 -d; echo
kubectl -n code-runner get secret app-secret -o jsonpath='{.data.MONGO_INITDB_ROOT_PASSWORD}' | base64 -d; echo
```
