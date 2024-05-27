#!/bin/bash
if [ -z "$DOMAIN" ];
then
  echo "Script should be run with DOMAIN var: $ DOMAIN=... ./generate.sh"
else
  # Ref.: https://www.baeldung.com/openssl-self-signed-cert
  echo "1. Generate a private key (not encrypted, add -des3 to encrypt it)"
  openssl genrsa -out "$DOMAIN".key 2048

  echo "2. Generate a Certificate Signing Request (CSR)"
  openssl req -key "$DOMAIN".key -new -out "$DOMAIN".csr -subj "/C=FR/CN=$DOMAIN"

  echo "3. Generate a Self-Signed Certificate (ie signed with its own private key)"
  if [ -z "$EXT" ];  # root.ext for instance
  then
    openssl x509 -signkey "$DOMAIN".key -in "$DOMAIN".csr -req -days 1095 -out "$DOMAIN"_self-signed.crt
  else
    echo "Running with ext file $EXT"
    openssl x509 -signkey "$DOMAIN".key -in "$DOMAIN".csr -req -days 1095 -out "$DOMAIN"_self-signed.crt -extfile $EXT
  fi
  # 1-3. One-liner for Self-Signed Certificate (remove -nodes to encrypt key)
  # openssl req -newkey rsa:2048 -nodes -keyout "$DOMAIN".key -x509 -days 1095 -out "$DOMAIN".crt

  echo "4. Send $DOMAIN.csr to your CA so they can sign it."
fi
