# install Prometheus operator
```shell
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
```

```shell
helm install prometheus-operator prometheus-community/kube-prometheus-stack
```

```shell
kubectl apply -f supervision/prometheus.yml
kubectl apply -f supervision/alertmanager.yml
```

*this didn't work*

kubectl apply -f prometheus-service.yml
kubectl apply -f ingress-admin.yml
*end*

***this works locally***

kubectl port-forward svc/prometheus-operated 9090:9090

