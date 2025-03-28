# Local run
The certificate in `certs/` is the one of the integration carto.
The `certs/rootCA.crt` is IGC TEST.

## Back-end run
```bash
HUB_URL=amqps://messaging.integration.hub.esante.gouv.fr CARTO_PASSPHRASE=... CLIENT_ID=fr.health.carto VHOST=15-GPS_v1.2 node index.js
```