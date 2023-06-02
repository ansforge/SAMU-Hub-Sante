# Hub Santé
_Site web vitrine_

## Deploy
```bash
docker buildx build --platform linux/amd64 -t romainfd/hub-web:latest .
docker push romainfd/hub-web:latest
# Make sure you are on correct Kubernetes context
kubectl replace --force -f ../SAMU-Hub-Sante/hub/infra/web.yaml
```

## Certificates
### Self-signed certificate
```bash
openssl req -x509 -nodes -days 9999 -newkey rsa:2048 -keyout certs/self-signed/ingress-tls.key -out certs/self-signed/ingress-tls.crt
```

### Specify as landing-cert
```bash
kubectl create secret tls landing-cert --key certs/sslforfree/private.key --cert certs/sslforfree/certificate.crt -o yaml
```
