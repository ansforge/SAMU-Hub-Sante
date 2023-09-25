## kube dashboard
*all relatives paths start from /hub/infra*

see https://help.ovhcloud.com/csm/en-gb-public-cloud-kubernetes-install-kubernetes-dashboard?id=kb_article_view&sysparm_article=KB0049828

```shell
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml

kubectl apply -f dashboard/dashboard-service-account.yml
kubectl apply -f dashboard/dashboard-cluster-role-binding.yml
kubectl apply -f dashboard/service-account-token.yml
```

### Get token and use proxy
````shell
# Retrieve service account token from CLI
kubectl -n kubernetes-dashboard describe secret $(kubectl -n kubernetes-dashboard get secret | grep admin-user-token | awk '{print $1}')

# Start proxy
kubectl proxy

# At this point, if your brower has not open a new tab by itself, you can browse to http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/#/login and paste the generated token
````
