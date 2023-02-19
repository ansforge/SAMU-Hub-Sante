```bash
Ref.: https://www.rabbitmq.com/ssl.html#java-client-connecting-with-peer-verification
keytool -import -alias "$hubCertificateNameInKeyStore" -file "$pathToHubCertificate" -keystore "$pathToRabbitstore"
```
