#!/bin/bash
if [ -z "$DOMAIN" ];
then
  echo "Script should be run with DOMAIN var: $ DOMAIN=... ./sign.sh"
else
  ROOT_CA="rootCA"
  if [ "$CA" = "bbo" ]
  then
    ROOT_CA="bbo-rootCA"
  fi
  if [ "$CA" = "CATrue" ]
  then
    ROOT_CA="rootCATrue"
  fi
  if [ "$DOMAIN" = "hub.preprod" ];
  then
    # Additional SAN can be added in hub.ext
    # Ref.: https://www.baeldung.com/openssl-self-signed-cert
    # Ref.: https://sleeplessbeastie.eu/2021/04/26/how-to-define-ip-address-inside-multi-domain-ssl-certificate/
    openssl x509 -req -CA "$ROOT_CA".crt -CAkey "$ROOT_CA".key -in "$DOMAIN".csr -out "$DOMAIN".crt -days 365 -CAcreateserial -extfile "$DOMAIN".ext
    echo "> Copy hub.crt and hub.key into hub/rabbitmq/certs"
    echo "> Delete current TLS secret: kubectl delete secret tls-secret"
    echo "> Recreate new secret (from hub/infra): kubectl create secret tls tls-secret --cert=../../hub/rabbitmq/certs/hub.crt --key=../../hub/rabbitmq/certs/hub.key"
    echo "> Update Pods to use new secret: kubectl delete pods -l app.kubernetes.io/component=rabbitmq"
    echo "> Add certificate to Client truststore (from repository root): keytool -import -alias RabbitMQHubSante -file hub/rabbitmq/certs/hub.crt -keystore certs/trustStore"
    echo "> Copy trustStore to be used in Dispatcher (from repository root): cp certs/trustStore dispatcher/src/main/jib/certs/"
  else
    openssl x509 -req -CA "$ROOT_CA".crt -CAkey "$ROOT_CA".key -in "$DOMAIN".csr -out "$DOMAIN".crt -days 365 -CAcreateserial -extfile client.ext
  fi
fi