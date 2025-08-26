#!/bin/bash
if [ -z "$DOMAIN" ];
then
  echo "Script should be run with DOMAIN var: $ DOMAIN=... ./sign.sh"
else
  ROOT_CA="rootCA"
  if [ "$DOMAIN" = "rabbitmq" ];
  then
    openssl x509 -req -CA "$ROOT_CA".crt -CAkey "$ROOT_CA".key -in "$DOMAIN".csr -out "$DOMAIN".crt -days 365 -CAcreateserial -extfile rabbitmq.ext
  else
    openssl x509 -req -CA "$ROOT_CA".crt -CAkey "$ROOT_CA".key -in "$DOMAIN".csr -out "$DOMAIN".crt -days 365 -CAcreateserial -extfile client.ext
  fi
fi
