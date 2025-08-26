We need certificates to run the Dispatcher Integration tests

## Self_signed root certificate
We first need a self-signed certificate to sign client certificate requests; it will also be embedded in the Java truststore.

```bash
EXT=root.ext DOMAIN=root ./generate.sh
```

## Generate client csr and private keys
To generate a private key, a CSR (Certificate Signing Request) and a self-signed certificate, run
```bash
DOMAIN=<client_name> ./generate.sh
```
for example
```bash
DOMAIN=dispatcher ./generate.sh
```
To run Dispatcher tests, we need the following client_names : rabbitmq, dispatcher, fr.health.samuA and fr.health.samuB.

## Sign
Then we need to sign the generated CSRs to create the signed .crt files
```bash
cd CA;
# copy csr file only to CA/ folder
# we can delete the self_signed generated cert file
DOMAIN=<client_name> ./sign.sh
```
It will generate a $DOMAIN.crt file.
Now we have a $DOMAIN.crt and a $DOMAIN.key file for each client

## Generate p12 files for Java clients
for each Java client (dispatcher, samuA, samuB), run
```bash
openssl pkcs12 -export -in dispatcher.crt -inkey dispatcher.key -out dispatcher.test.p12
openssl pkcs12 -export -in fr.health.samuA.crt -inkey fr.health.samuA.key -out samuA.p12
openssl pkcs12 -export -in fr.health.samuB.crt -inkey fr.health.samuB.key -out samuB.p12 
```

## Add to truststore
We need to add the issuer in the truststore
```bash
keytool -import -alias rabbitmq -file rabbitmq.crt -keystore trustStore -passin trustStore -noprompt
```
