# PerfTest
Ref: https://perftest.rabbitmq.com/

Ceci est une implémentation basique d'un test de perf RabbitMQ contre notre infrastructure.

En l'état, il est utilisable uniquement sur l'environnement bac à sable.

Pour l'utiliser ailleurs, il faudrait modifier l'url du broker et des certificats adaptés.

Il existe une image Docker qui pourra simplifier le déploiement et éviter d'embarquer le jar ; 
il faudra juste s'assurer qu'on peut surcharger le contexte SSL comme on le fait avec le jar.

## Usage :

```bash
java -Djavax.net.ssl.keyStore=local_test.p12 -Djavax.net.ssl.keyStorePassword=certPassword \
-Djavax.net.ssl.keyStoreType=PKCS12 -Djavax.net.ssl.trustStore=trustStore \
-Djavax.net.ssl.trustStorePassword=trustStore -Djavax.net.ssl.trustStoreType=JKS -jar perf-test.jar \
-x 2 -y 0 -e "hubsante" -k "fr.health.samuA" -se true --rate 160 \
-h amqps://messaging.hub.esante.gouv.fr:5671 -B RC-EDA.json -T application/json -mp deliveryMode=2 -p true
```

*Explication des options :*
- -D<jvm_opt> configuration TLS
- -x : nombre de publishers
- -y : nombre de consumers (0 car le Dispatcher est déjà en écoute)
- -e : exchange cible (par défaut, perfTest crée une queue et publie dessus, ce n'est pas ce que nous voulons tester)
- -k : routing key (par défaut, perfTest en génère une au hasard, nous voulons tester une RK spécifique qui match le certificat)
- -se : activation de l'authentification SASL (par défaut, perfTest ne présente pas de certif client et trust tous les CAs)
- --rate : limite du nombre de messages envoyés par publisher et par seconde. Sans limite, cela peut monter à 80 k / seconde !)
- -h : host du rabbitMQ
- -B : fichier source du message body
- -T : content type
- -mp : message_properties : permet de passer des propriétés additionnelles attendues par le Dispatcher (deliveryMode=2 => persistent)
- -p : predeclared : perfTest ne cherche pas à créer les objets RabbitMQ (l'exchange dans notre cas)

## Contrôle
depuis l'IHM RabbitMQ, on peut suivre le taux de débit des messages sur l'exchange et la file dispatch (temps de traitement côté broker => aucune latence)

depuis l'IHM Prometheus, on peut suivre la jauge des messages non dépilés sur la file d'écoute du Dispatcher (rabbitmq_queue_messages{queue="dispatch"})

=> Contrôle de la latence côté Dispatcher