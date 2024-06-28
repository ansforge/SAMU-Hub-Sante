# Procédure de renouvellement des certificats des tests embarqués

## depuis le répertoire certs/CA
Générer une csr, une key, puis signer le certificat, puis générer le p12 de chaque client.

A réaliser pour :
    [ ] samuA
    [ ] samuB
    [ ] dispatcher

```bash
export CLIENT=samuA
export DOMAIN=fr.health.$CLIENT
export CLIENT_PATH="../../hub/dispatcher/src/test/resources/config/certs/$CLIENT"

../generate.sh
mv ../$DOMAIN.csr .
mv ../$DOMAIN.key .
./sign.sh

mv $DOMAIN.crt ../../hub/dispatcher/src/test/resources/config/certs/$CLIENT/
mv $DOMAIN.key ../../hub/dispatcher/src/test/resources/config/certs/$CLIENT/
rm $DOMAIN.csr

openssl pkcs12 -export -in "$CLIENT_PATH/$CLIENT.crt" -inkey "$CLIENT_PATH/$CLIENT.key" -out "$CLIENT_PATH/$CLIENT.p12"
```

## Générer les certificats serveur RabbitMQ + le trustStore
Même chose qu'au-dessus, sans le p12. A la place :
```bash
# le truststore
cat "$CLIENT_PATH/$CLIENT.crt" | keytool -import -alias rabbitmq -trustcacerts -keystore trustStore -storetype PKCS12 -storepass trustStore -noprompt
```