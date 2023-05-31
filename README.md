# Hub Santé
_Site web vitrine_

## Deploy
docker buildx build --platform linux/amd64 -t romainfd/hub-web:latest .
docker push romainfd/hub-web:latest
kubectl replace --force -f ../SAMU-Hub-Sante/hub/infra/web.yaml
