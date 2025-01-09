# Setup

Prerequist: 
- certificate
- key
- trustStore

In `/client`, create a .env file with the following content:

```
EXCHANGE_NAME=hubsante
HUB_HOSTNAME=messaging.bac-a-sable.hub.esante.gouv.fr
VHOST=15-15_v2.0
HUB_PORT=5671
KEY_PASSPHRASE=<passphrase of the .p12 key>
CERT_PATH=<path/to/.p12/key> 
TRUST_STORE_PASSWORD=<password of the trustStore>
TRUST_STORE_PATH=<path/to/trustore>
```

# Run

At the root of the repository (not in `/client`), run :
`./gradlew :client:run --args='<client_id>.message json'`