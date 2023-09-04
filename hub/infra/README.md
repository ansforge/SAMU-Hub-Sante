# Clusters
## Minikube local cluster
To build a local Kubernetes cluster on your laptop and run the `kubectl` commands below, we recommend using Minikube ([install](https://minikube.sigs.k8s.io/docs/start/) / [tutorial](https://kubernetes.io/docs/tutorials/hello-minikube/)).
```bash
# Start minikube cluster with Docker | Ref.: https://minikube.sigs.k8s.io/docs/drivers/docker/
minikube start --driver=docker
```

## OVH cluster
While the OVH cluster is online, its access is limited to the Hub Sante team. 
As an editor, you won't be able to run the `kubectl` commands below on this cluster.

## Switch between multiple contexts
- https://github.com/ahmetb/kubectx
- https://www.howtogeek.com/devops/how-to-quickly-switch-kubernetes-contexts-with-kubectx-and-kubens/
- https://medium.com/@ahmetb/mastering-kubeconfig-4e447aa32c75

## Delete
> **Warning**
> Do not run in production as this would delete the LoadBalancer and make us lose the IP.
```
# DO NOT USE IN PRODUCTION
kubectl delete -f rabbitmq.yaml

minikube delete --profile minikube 
```

## Watch cluster state
```bash
# Watch cluster deployment (Running from scratch should take a few minutes)
watch kubectl get all
```

# RabbitMQ Kubernetes Operator
## Setup
Ref.: https://www.rabbitmq.com/kubernetes/operator/quickstart-operator.html
```bash
# Install the RabbitMQ Cluster Operator
kubectl apply -f "https://github.com/rabbitmq/cluster-operator/releases/latest/download/cluster-operator.yml"

# Create TLS secrets
# Ref.: https://www.rabbitmq.com/kubernetes/operator/using-operator.html#one-way-tls
# Ref.: https://www.rabbitmq.com/kubernetes/operator/using-operator.html#mutual-tls
kubectl create secret tls tls-secret --cert=../rabbitmq/certs/hub.crt --key=../rabbitmq/certs/hub.key
kubectl create secret generic ca-secret --from-file=ca.crt=../rabbitmq/certs/rootCA.crt

# Create definitions.json ConfigMap
# Ref.: https://github.com/rabbitmq/cluster-operator/blob/main/docs/examples/import-definitions/setup.sh
kubectl create configmap definitions --from-file=../rabbitmq/definitions.json

# Find/build the correct Custom Resource Definition yaml and install it
# https://github.com/rabbitmq/cluster-operator/tree/main/docs/examples
kubectl apply -f rabbitmq.yaml
```

## Updates
### Definitions update
```bash
kubectl delete configmap definitions          
kubectl create configmap definitions --from-file=../rabbitmq/definitions.json
# Trigger definitions reload (might need to be done several time as there seem to be a delay for the new config map to take effect)
kubectl exec -it rabbitmq-server-0 -- rabbitmqctl import_definitions /tmp/rabbitmq/config/definitions.json
# Or full Pod restart (to be avoided)
kubectl delete pods -l app.kubernetes.io/component=rabbitmq
```

# Prometheus operator
```shell
# no need to do this every time
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
```

```shell
helm install prometheus-operator prometheus-community/kube-prometheus-stack
```

```shell
kubectl apply -f rabbitmq-servicemonitor.yml
```

### Access Prometheus UI
```shell
# if using minikube
kubectl port-forward svc/prometheus-operated 9090:9090
```

# Dispatcher
```bash
# Build and upload image to registry accessible in OVH
gradle jib --image=romainfd/dispatcher:latest
# Equivalent to
# gradle jibDockerBuild --image=romainfd/dispatcher:latest
# docker push romainfd/dispatcher:latest

# Apply Deployment
kubectl create secret generic dispatcher-tls-passwords --from-literal=truststore-password=trustStore --from-literal=keystore-password='${replaceMe}'
kubectl create secret generic dispatcher-p12 --from-file=../../certs/users/dispatcher/dispatcher.p12
kubectl create secret generic trust-store --from-file=../../certs/trustStore

kubectl apply -f dispatcher.yaml
# Reapply deployment with new image
kubectl replace --force -f dispatcher.yaml

# Get Pod logs
kubectl logs -l app=dispatcher --prefix --tail -1 -f
```

## Local development
```bash
# Localhost exposition of the Hub
## From Minikube
minikube tunnel
## From OVH
kubectl port-forward "service/rabbitmq" 15672
kubectl port-forward "service/rabbitmq" 5671

# Run the rest of the pipeline locally (with dispatcher first to create exchange / queues / bindings
gradle bootRun --args='--spring.profiles.active=local,rfo'
CLIENT_ID=fr.health.samuA; gradle -Pmain=com.hubsante.ConsumerRun run --args "$CLIENT_ID.message json"
CLIENT_ID=fr.fire.nexsis.sdisZ; gradle -Pmain=com.hubsante.ConsumerRun run --args "$CLIENT_ID.ack xml"
CLIENT_ID=fr.fire.nexsis.sdisZ; gradle -Pmain=com.hubsante.ProducerRun run --args "$CLIENT_ID src/main/resources/sdisZ_to_samuA.xml"
```

# Web Load Balancer & Ingress
All webpages are accessible behind a shared Load Balancer and Ingress
```bash
# Build Nginx Ingress Controller -> creates a Load Balancer
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.0/deploy/static/provider/cloud/deploy.yaml

# Deploy Ingress
kubectl apply -f web/ingress.yaml
```

## Landing page website
```bash
# Deploy Service and Deployment
kubectl apply -f web/landing.yaml
```

# Debugging
## RabbitMQ and Cluster Operator
```bash
# Access RabbitMQ Cluster Operator logs
kubectl logs -n rabbitmq-system -l app.kubernetes.io/name=rabbitmq-cluster-operator --prefix --tail -1 -f

# Access RabbitMQ Pod logs
kubectl logs -l app.kubernetes.io/component=rabbitmq --prefix --tail -1 -f

# SSH into RabbitMQ Pod (rabbitmqctl, ... commands available)
kubectl exec --stdin --tty rabbitmq-server-0 -- /bin/bash
```

## Dispatcher logs
```bash
# Access Dispatcher Pod logs
kubectl logs -l app=dispatcher --prefix --tail -1 -f

# Get Pods events (in cas stuck in ContainerCreating for instance)
# Ref.: https://serverfault.com/a/730746
kubectl describe pods -l app=dispatcher
# All events
kubectl get events --all-namespaces  --sort-by='.metadata.creationTimestamp'
```


## [BusyBox](https://en.wikipedia.org/wiki/BusyBox) Investigation Pod
```bash
# SSH into BusyBox Pod to test Services, accesses, ...
kubectl run -it --rm --restart=Never busybox --image=gcr.io/google-containers/busybox sh
```

## Services, Ingress Controller and Ingress
Ref.: https://medium.com/@ManagedKube/kubernetes-troubleshooting-ingress-and-services-traffic-flows-547ea867b120
```bash
# Ingress Controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller --prefix --tail -1 -f

# SSH into Ingress Controller Pod (to check it can reach Services)
kubectl get pods -n ingress-nginx  # -> collect Controller Pod name
kubectl exec -n ingress-nginx --stdin --tty ingress-nginx-controller-6cc5ccb977-2hwk2 -- /bin/bash
$ curl localhost/rabbitmq
```

# Deploying
For API deployment, the best deployment strategy is ["Blue/Green"](https://blog.container-solutions.com/kubernetes-deployment-strategies#kubernetes-blue-green).

## RabbitMQ
However, we don't have direct access to RabbitMQ Service so this is not so easy.
Therefore, we just do a Kubernetes apply.
- Clear all the Secrets and ConfigMaps (uncomment based on needs)
```bash
kubectl delete configmap definitions          
# kubectl delete secret tls-secret
# kubectl delete secret ca-secret
```
- Run [RabbitMQ setup](#setup).
- Trigger a [definitions.json update](#definitions-update)
```bash
kubectl exec -it rabbitmq-server-0 -- rabbitmqctl import_definitions /tmp/rabbitmq/config/definitions.json
```
- Check [RabbitMQ logs](#rabbitmq-and-cluster-operator) to see updates

# Dispatcher
["Blue/Green"](https://blog.container-solutions.com/kubernetes-deployment-strategies#kubernetes-blue-green) could be implemented. 
For the moment, simple image replace :
- Build and publish new image (if needed as this is done automatically - with tags - on a GitHub release), new secrets and reapply deployment with new image: see [above](#dispatcher).
- Get [Pod logs](#dispatcher-logs)
