## RabbitMQ Kubernetes Operator
Ref.: https://www.rabbitmq.com/kubernetes/operator/quickstart-operator.html
```bash
# Start minikube cluster with Docker | Ref.: https://minikube.sigs.k8s.io/docs/drivers/docker/
minikube start --driver=docker

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

# Watch cluster deployment (Running from scratch should take a few minutes)
watch kubectl get all

# Localhost exposition of the Hub
## From Minikube
minikube tunnel
## From OVH
kubectl port-forward "service/rabbitmq" 15672
kubectl port-forward "service/rabbitmq" 5671

# Run the rest of the pipeline (with disptacher first to create exchange / queues / bindings
gradle bootRun --args='--spring.profiles.active=local,rfo'
CLIENT_ID=Target; gradle -Pmain=com.hubsante.ConsumerRun run --args "$CLIENT_ID.in.message"
CLIENT_ID=Origin; gradle -Pmain=com.hubsante.ConsumerRun run --args "$CLIENT_ID.in.ack"
CLIENT_ID=Origin; gradle -Pmain=com.hubsante.ProducerRun run --args "$CLIENT_ID.out.message src/main/resources/createEventMessage.json"
```

### Next steps
- [ ] Support RabbitMQ persistency (with persistent volume & persistent volume claim)

## Configuration
### Switch between multiple contexts
- https://github.com/ahmetb/kubectx
- https://www.howtogeek.com/devops/how-to-quickly-switch-kubernetes-contexts-with-kubectx-and-kubens/
- https://medium.com/@ahmetb/mastering-kubeconfig-4e447aa32c75

## Delete
```
kubectl delete -f rabbitmq.yaml

minikube delete --profile minikube 
```

## Debugging
```bash
Ref.: https://medium.com/@ManagedKube/kubernetes-troubleshooting-ingress-and-services-traffic-flows-547ea867b120
```bash
# Access Operator logs
kubectl logs -n rabbitmq-system -l app.kubernetes.io/name=rabbitmq-cluster-operator --prefix --tail -1 -f

# Access Pod logs
kubectl logs -l app.kubernetes.io/component=rabbitmq --prefix --tail -1 -f

# SSH into RabbitMQ Pod (rabbitmqctl, ... commands available)
kubectl exec --stdin --tty rabbitmq-server-0 -- /bin/bash

# SSH into BusyBox Pod to test Services, accesses, ...
kubectl run -it --rm --restart=Never busybox --image=gcr.io/google-containers/busybox sh

# Ingress Controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller --prefix --tail -1 -f

# SSH into Ingress Controller Pod (to check it can reach Services)
kubectl get pods -n ingress-nginx  # -> collect Controller Pod name
kubectl exec -n ingress-nginx --stdin --tty ingress-nginx-controller-6cc5ccb977-2hwk2 -- /bin/bash
$ curl localhost/rabbitmq
```
