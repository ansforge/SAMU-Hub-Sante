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


# Testing generated certificates
## Purpose
We want to be able to easily test if generated certificates can successfully be used to connect to the different instances of the Hub Santé.

We can only do it for certificates we have the private key for.

Because the CA bundle used by RabbitMQ may differ between environments, we need to test each environment separately.

We adopt the following folder structure:
environments
- sandbox
- preprod
- prod

In each of these environments lays a CA-bundle pem and several client folders.
In each of these client folders lay .crt and .key files with the same name as the folder.

A python script iterates on the environment and, for each client, tries to connect to the RabbitMQ instance corresponding to the environment (defined in the script).

## Usage
```bash
pip install -r requirements.txt
python test-rabbitmq-certs.py
```