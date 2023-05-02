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
kubectl create secret tls tls-secret --cert=../../rabbitmq/certs/hub.crt --key=../../rabbitmq/certs/hub.key
kubectl create secret generic ca-secret --from-file=ca.crt=../../rabbitmq/certs/rootCA.crt

# Create definitions.json ConfigMap
# Ref.: https://github.com/rabbitmq/cluster-operator/blob/main/docs/examples/import-definitions/setup.sh
kubectl create configmap definitions --from-file=../../rabbitmq/definitions.json

# Find/build the correct Custom Resource Definition yaml and install it
# https://github.com/rabbitmq/cluster-operator/tree/main/docs/examples
kubectl apply -f rabbitmq.yaml

# Watch cluster deployment (Running from scratch should take a few minutes)
watch kubectl get all

# MANAGEMENT UI | Ref.: https://www.rabbitmq.com/kubernetes/operator/quickstart-operator.html
username="$(kubectl get secret rabbitmq-default-user -o jsonpath='{.data.username}' | base64 --decode)"
echo "username: $username"
password="$(kubectl get secret rabbitmq-default-user -o jsonpath='{.data.password}' | base64 --decode)"
echo "password: $password"
kubectl port-forward "service/rabbitmq" 15672

# FORWARD RABBITMQ
## M1. Local port forward 
kubectl port-forward "service/rabbitmq" 5671

## M2. INGRESS
### Enabling Ingress Controller | Ref.: https://kubernetes.io/docs/tasks/access-application-cluster/ingress-minikube/
minikube addons enable ingress

### Update TCP Config Map to support AMQPS
# Ref.: https://minikube.sigs.k8s.io/docs/tutorials/nginx_tcp_udp_ingress/#update-the-tcp-andor-udp-services-configmaps
kubectl patch configmap tcp-services -n ingress-nginx --patch '{"data":{"5671":"default/rabbitmq:5671"}}'
# Check ConfigMap was updated
kubectl get configmap tcp-services -n ingress-nginx -o yaml

### Patch Ingress Controller to listen on port 5671
# Ref.: https://minikube.sigs.k8s.io/docs/tutorials/nginx_tcp_udp_ingress/#patch-the-ingress-nginx-controller
kubectl patch deployment ingress-nginx-controller --patch "$(cat ingress-nginx-controller-patch.yaml)" -n ingress-nginx
# Patch Ingress Service too so `minikube tunnel` exposes the port -> NOT WORKING...
kubectl patch svc ingress-nginx-controller --patch "$(cat ingress-nginx-controller-svc-patch.yaml)" -n ingress-nginx
# Directly expose the Ingress Service 
minikube service ingress-nginx-controller -n ingress-nginx
# -> fails on handshake termination... 
# Patch Ingress Controller to enable SSL passthrough | Ref.: https://github.com/kubernetes/minikube/issues/6403#issuecomment-1307883410
kubectl patch deployment -n ingress-nginx ingress-nginx-controller --type='json' -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value":"--enable-ssl-passthrough"}]'
# Ref.: https://arunsworld.medium.com/ssl-passthrough-via-kubernetes-ingress-b3eaf3c7c9da

## Next steps
# Do the same logic for a simple AMQP 5672 RabbitMQ cluster and see if it fails
# Investigate why connection is closed


### Ingress for Dashboard UI -> http://localhost/rabbitmq/
kubectl apply -f ingress.yaml
# Check Ingress is up
kubectl get ingress

# End of FORWARD RABBITMQ

# Run the rest of the pipeline (with disptacher first to create exchange / queues / bindings
gradle bootRun --args='--spring.profiles.active=local,rfo'
CLIENT_ID=Target; gradle -Pmain=com.hubsante.ConsumerRun run --args "$CLIENT_ID.in.message"
CLIENT_ID=Origin; gradle -Pmain=com.hubsante.ConsumerRun run --args "$CLIENT_ID.in.ack"
CLIENT_ID=Origin; gradle -Pmain=com.hubsante.ProducerRun run --args "$CLIENT_ID.out.message src/main/resources/createEventMessage.json"
```

### Delete
```
kubectl delete --all rabbitmqclusters.rabbitmq.com

minikube delete --profile minikube 
```

### Debugging
Ref.: https://medium.com/@ManagedKube/kubernetes-troubleshooting-ingress-and-services-traffic-flows-547ea867b120
```bash
# Access Operator logs
kubectl logs -n rabbitmq-system -l app.kubernetes.io/name=rabbitmq-cluster-operator --prefix --tail -1 -f

# Access Pod logs
kubectl logs -l app.kubernetes.io/component=rabbitmq --prefix --tail -1 -f

# SSH into RabbitMQ Pod (rabbitmqctl, ... commands available)
kubectl exec --stdin --tty rabbitmq-server-0 -- /bin/bash

# Ingress Controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller --prefix --tail -1 -f

# SSH into Ingress Controller Pod (to check it can reach Services)
kubectl get pods -n ingress-nginx  # -> collect Controller Pod name
kubectl exec -n ingress-nginx --stdin --tty ingress-nginx-controller-6cc5ccb977-2hwk2 -- /bin/bash
$ curl localhost/rabbitmq
```

### Next steps
- [ ] Support RabbitMQ persistency (with perisistent volume & persistent volume claim)
