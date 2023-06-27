# Scripts utiles
## Kubernetes & Docker (RabbitMQ & Hub)
Pour plus de détails sur l'instanciation en local du Hub Santé, veuillez vous référer au dossier [hub/infra](../hub/infra/README.md).

## Consumer / Producer
### Linux / MAC OS
```bash
## Adapter ConsumerRun / ProducerRun HUB_HOSTNAME pour utiliser localhost ou hubsante.esante.gouv.fr

## Lancer un Consumer sur $CLIENT_ID.in.message
# Shell 1
CLIENT_ID=fr.health.samuA; gradle run --args "$CLIENT_ID.in.message json"

## Lancer un Consumer sur $CLIENT_ID.in.ack
#Shell 2
CLIENT_ID=fr.fire.nexsis.sdisZ; gradle run --args "$CLIENT_ID.in.ack xml"

## Envoyer un message sur $CLIENT_ID.out.message avec un Producer
# Shell 3
CLIENT_ID=fr.fire.nexsis.sdisZ; gradle -Pmain=com.hubsante.ProducerRun run --args "$CLIENT_ID.out.message xml src/main/resources/sdisZ_to_samuA.xml"

## Vous pouvez également tester le fonctionnement avec un client communiquant en XML avec NexSIS
## en remplaçant le CLIENT_ID samu par "fr.health.samuB"
```

### Windows
```bash
## Depuis le répertoire ./client

## Lancer un Consumer sur $CLIENT_ID.in.message
set CLIENT_ID=fr.health.hub.samuA
gradle run --args "%CLIENT_ID%.in.message json"

## Lancer un Consumer sur $CLIENT_ID.in.ack
set CLIENT_ID=fr.fire.nexsis.sdisZ
gradle run --args "%CLIENT_ID%.in.ack xml"

## Envoyer un message sur $CLIENT_ID.out.message avec un Producer
set CLIENT_ID=fr.fire.nexsis.sdisZ
gradle -Pmain=com.hubsante.ProducerRun run --args "$CLIENT_ID.out.message xml src/main/resources/sdisZ_to_samuA.xml"

## Vous pouvez également tester le fonctionnement avec un client communiquant en XML avec NexSIS
## en remplaçant le CLIENT_ID samu par "fr.health.samuB"
```

### Certificats
```bash
## Générer votre CSR (en remplaçant la valeur client de $DOMAIN par votre identifiant client) 
cd certs
DOMAIN=client ./generate.sh
## Après transmission de votre CSR à l'équipe Hub Santé, vous recevrez un certificat signé par l'AC (en .crt)

## Conversion en .p12
export DOMAIN=client; openssl pkcs12 -inkey "$DOMAIN".key -in "$DOMAIN".crt -export -out "$DOMAIN".p12

## Ajouter le Hub au trustStore utilisé par le client
keytool -import -alias RabbitMQHubSante -file hub/rabbitmq/certs/hub.crt -keystore certs/trustStore

## Voir les détails d'un certificat
export DOMAIN=client; openssl x509 -text -noout -in "$DOMAIN".crt

## Vérifier qu'un certificat est bien signé par la CA
openssl verify -CAfile certs/CA/rootCA.crt certs/client.crt
```

### TLS
Ref.: https://www.rabbitmq.com/troubleshooting-ssl.html
```bash
## Créer un serveur TLS avec les certificats du Hub
openssl s_server -accept 8443 -cert rabbitmq/certs/hub.crt -key rabbitmq/certs/hub.key -CAfile rabbitmq/certs/rootCA.crt

## Créer un client se connectant en TLS avec les certificats clients au serveur TLS
openssl s_client -connect localhost:8443 \
  -cert certs/client.crt -key certs/client.key -CAfile certs/CA/rootCA.crt \
  -verify 8

## Créer un client se connectant en TLS avec les certificats clients au RabbitMQ
### Va donner un `handshake_timeout`dans les logs RabbitMQ car aucune donnée n'est transmise
### Mais une ligne similaire devrait apparaitre : [info] <0.769.0> accepting AMQP connection <0.769.0>
openssl s_client -showcerts -connect localhost:5671 -cert certs/client.crt -key certs/client.key -CAfile certs/CA/rootCA.crt

## Créer un client se connectant en TLS1.1 avec les certificats clients au RabbitMQ -> retourne une `alert protocol version`
openssl s_client -connect localhost:5671 -cert certs/client.crt -key certs/client.key -CAfile certs/CA/rootCA.crt -tls1.1 

## Voir le certificat du serveur | Ref.: https://stackoverflow.com/a/7886248/10115198
echo | \
  openssl s_client -showcerts -connect hubsante.esante.gouv.fr:5671 -cert certs/client.crt -key certs/client.key -CAfile certs/CA/rootCA.crt | \
  openssl x509 -text

## Après SSH dans le noeud RabbitMQ (voir section Scripts utiles > Docker), voir les `listeners` actifs
rabbitmq-diagnostics status # Section Listeners doit avoir une ligne sur le port 5671
```

## Génération des classes basées sur les spécifications AsyncAPI
Plus de détails sont disponibles directement dans [`models/`](../models/README.md)
```bash
## Générer les classes
cd models; npm run generate

## Afficher localement la documentation sur http://localhost:8000
cd docs/specs; python -m http.server 
```
