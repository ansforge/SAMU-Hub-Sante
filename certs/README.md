# Certificates
## Generate 
To generate a private key, a CSR (Certificate Signing Request) and a self-signed certificate, run 
```bash
DOMAIN=client ./generate.sh
```

## Sign
To get a certificate signed by the Hub Santé CA, send the CSR to ANS team so they can run 
```bash
cd CA;
DOMAIN=client ./sign.sh
```

## Add to truststore
To add a certificate in a truststore, run
```bash
Ref.: https://www.rabbitmq.com/ssl.html#java-client-connecting-with-peer-verification
keytool -import -alias "$hubCertificateNameInKeyStore" -file "$pathToHubCertificate" -keystore "$pathToRabbitstore"
```
