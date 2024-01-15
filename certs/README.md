# Certificates
## Generate 
To generate a private key, a CSR (Certificate Signing Request) and a self-signed certificate, run 
```bash
DOMAIN=client ./generate.sh
```

# ANS
## Generate a CA
Needed for Topology Operator for instance as it checks its CA have SIGN rights
```bash
EXT=root.ext DOMAIN=root ./generate.sh
```

## Sign
To get a certificate signed by the Hub Santé CA, send the CSR to ANS team so they can run 
```bash
cd CA;
CA=CATrue DOMAIN=client ./sign.sh
```

## Add to truststore
To add a certificate in a truststore, run
```bash
Ref.: https://www.rabbitmq.com/ssl.html#java-client-connecting-with-peer-verification
keytool -import -alias "$hubCertificateNameInKeyStore" -file "$pathToHubCertificate" -keystore "$pathToRabbitstore"
```
