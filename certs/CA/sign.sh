#!/bin/bash
if [ -z "$DOMAIN" ];
then
  echo "Script should be run with DOMAIN var: $ DOMAIN=... ./sign.sh"
else
  # Ref.: https://www.baeldung.com/openssl-self-signed-cert
  openssl x509 -req -CA rootCA.crt -CAkey rootCA.key -in "$DOMAIN".csr -out "$DOMAIN".crt -days 365 -CAcreateserial -extfile hub-sante.ext
fi