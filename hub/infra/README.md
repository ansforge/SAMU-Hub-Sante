## RabbitMQ Kubernetes Operator
Ref.: https://www.rabbitmq.com/kubernetes/operator/quickstart-operator.html
```bash
# Start minikube cluster
minikube start

# Install the RabbitMQ Cluster Operator
kubectl apply -f "https://github.com/rabbitmq/cluster-operator/releases/latest/download/cluster-operator.yml"

# Create TLS secrets
# Ref.: https://www.rabbitmq.com/kubernetes/operator/using-operator.html#one-way-tls
# Ref.: https://www.rabbitmq.com/kubernetes/operator/using-operator.html#mutual-tls
kubectl create secret tls tls-secret --cert=../../rabbitmq/certs/hub.crt --key=../../rabbitmq/certs/hub.key
kubectl create secret generic ca-secret --from-file=ca.crt=../../rabbitmq/certs/rootCA.crt

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
# ToDo(rfo): make this work as it now uses guest:guest
kubectl port-forward "service/rabbitmq" 15672

# FORWARD RABBITMQ
## M1. Local port forward 
kubectl port-forward "service/rabbitmq" 5671

## M2. INGRESS (work in progress)
# Adding Ingress | Ref.: https://kubernetes.io/docs/tasks/access-application-cluster/ingress-minikube/
minikube addons enable ingress
 
# Apply Ingress
kubectl apply -f ingress.yaml

# Check Ingress is up
kubectl get ingress

# Run the rest of the pipeline (with disptacher first to create exchange / queues / bindings
gradle bootRun --args='--spring.profiles.active=local,rfo'
CLIENT_ID=Target; gradle -Pmain=com.hubsante.ConsumerRun run --args "$CLIENT_ID.in.message"
CLIENT_ID=Origin; gradle -Pmain=com.hubsante.ConsumerRun run --args "$CLIENT_ID.in.ack"
CLIENT_ID=Origin; gradle -Pmain=com.hubsante.ProducerRun run --args "$CLIENT_ID.out.message src/main/resources/createEventMessage.json"
```

### Debugging
```bash
# Access Operator logs
kubectl -n rabbitmq-system logs -l app.kubernetes.io/name=rabbitmq-cluster-operator 

# Access Pod logs
kubectl logs pod/rabbitmq-server-0
```

### Next steps
- [ ] Add users with specific queue rights + authz with certificates + authn based on info -> remove guest:guest as default user
- [ ] Load resources (exchange / queue / ...) definitions from definitions.json 
- [ ] Support RabbitMQ persistency (with perisistent volume & persistent volume claim)
