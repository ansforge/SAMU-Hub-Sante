# Setup

Pre-requisite: use the documentation provided here : https://hub.esante.gouv.fr/pages/parcours.html (step 3), to generate:

- certificate
- key
- trustStore

In `/client`, create a .env file with the following content:

```
EXCHANGE_NAME=hubsante
HUB_HOSTNAME=messaging.bac-a-sable.hub.esante.gouv.fr
HUB_PORT=5671
VHOST=15-15_v2.0
KEY_PASSPHRASE=<passphrase of the .p12 key>
CERTIFICATE_PATH=<path/to/.p12/key>
TRUST_STORE_PASSWORD=<password of the trustStore>
TRUST_STORE_PATH=<path/to/trustore>
```

# Run

[Consumer]
In `/client`, run :
`./gradlew run --args='<client_id>.message json'`

[Producer]
In `/client`, run :
`./gradlew run -Pmain=com.examples.ProducerRun --args='<client_id> <path/to/message>'`

# Perform Manual Tests

1. Open https://bac-a-sable.hub.esante.gouv.fr/lrm
2. Select "ID du système utilisé": "fr.health.test.<denomination>"
3. Select "ID du système cible": <clientId>
4. Click on "LRM de Test"
5. Make sure to select the "vHost" corresponding to the "com.hubsante:models" dependency version defined in your codebase and in your environment variables (otherwise, you won't be able to receive / send messages)

If you ran "ConsumerRun", you can now send messages from the Bac à Sable and receive them locally. The received message is logged in your terminal.

If you ran "ProducerRun", you can now see the sent message to the Bac à Sable in the message queue.
