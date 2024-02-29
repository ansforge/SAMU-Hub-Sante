```bash
java -Djavax.net.ssl.keyStore=local_test.p12 -Djavax.net.ssl.keyStorePassword=certPassword \
-Djavax.net.ssl.keyStoreType=PKCS12 -Djavax.net.ssl.trustStore=trustStore \
-Djavax.net.ssl.trustStorePassword=trustStore -Djavax.net.ssl.trustStoreType=JKS -jar perf-test.jar \
-x 2 -y 0 -e "hubsante" --id "test1" -k "fr.health.samuA" -o "test.output" -se true --rate 160 \
-h amqps://messaging.hub.esante.gouv.fr:5671 -B RC-EDA.json -T application/json -mp deliveryMode=2 -p true
```