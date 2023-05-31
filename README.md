# Hub Santé
_Site web vitrine_

## Deploy
docker buildx build --platform linux/amd64 -t romainfd/hub-web:latest .
docker push romainfd/hub-web:latest
kubectl replace --force -f ../SAMU-Hub-Sante/hub/infra/web.yaml

## Certificates
### Self-signed certificate
openssl req -x509 -nodes -days 9999 -newkey rsa:2048 -keyout certs/self-signed/ingress-tls.key -out certs/self-signed/ingress-tls.crt

### Specify as landing-cert
kubectl create secret tls landing-cert --key certs/sslforfree/private.key --cert certs/sslforfree/certificate.crt -o yaml
