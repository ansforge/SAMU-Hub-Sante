# Hub Santé - LRM
_LRM basique afin de pouvoir tester l'envoi / réception de messages_

## Deploy
```bash
cp ../../models/csv_parser/schema.json schemas/
cp ../../models/csv_parser/example.json schemas/
docker buildx build --platform linux/amd64 -t romainfd/hub-lrm:latest .
docker push romainfd/hub-lrm:latest
# Make sure you are on correct Kubernetes context
kubectl replace --force -f ../../hub/infra/web/lrm.yaml
```
