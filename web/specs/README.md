# Hub Santé Specs
_Site web exposant les specifications AsyncAPI des modèles portés par le Hub Santé_

## Generate
This website is generated from the [csv_parser](../../models/csv_parser).

## Deploy
```bash
docker buildx build --platform linux/amd64 -t romainfd/hub-specs:latest .
docker push romainfd/hub-specs:latest
# Make sure you are on correct Kubernetes context
kubectl replace --force -f ../../hub/infra/web/specs.yaml
```