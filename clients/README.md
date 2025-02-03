# Projet Clients

## Description

Le projet Clients propose un [SDK Java](./src/main/java/com/hubsante) accompagné d'un ensemble de tutoriels détaillés. Ce SDK permet d'interagir avec le Hub Santé via un ensemble de méthodes prêtes à l'emploi pour :

- Se connecter et se déconnecter du Hub Santé,
- Publier des messages vers des files du Hub Santé,
- Écouter des messages en provenance du Hub Santé.

En complément, des [tutoriels spécifiques](./src/main/java/com/examples/) sont fournis pour illustrer l'utilisation du [SDK](./src/main/java/com/hubsante), ainsi qu'un ensemble de [guides dédiés à RabbitMQ](./src/main/java/com/tutorials/) pour une meilleure compréhension des principes fondamentaux.

## Fonctionnalité importante du SDK

Le SDK propose une configuration de rétablissement de connexion. Ce mécanisme vous protège contre les déconnexions involontaires et les interruptions temporaires de votre connexion avec RabbitMQ.

Il vous protège :

- Des déconnexions temporaires du réseau,
- Des timeouts sur les lectures du socket,
- Des pannes temporaires du serveur RabbitMQ,
- Des exceptions inattendues dans la boucle I/O.

Mais, il ne couvre pas les échecs de connexion initiale.

cf - https://www.rabbitmq.com/client-libraries/java-api-guide#recovery

Ce mécanisme vous évite d’avoir à gérer manuellement les déconnexions temporaires et assure une meilleure résilience de votre application face aux perturbations réseau.

## Tutoriels inclus

### Prise en main du SDK, exemples d'utilisation :

- Installation et configuration,
- Gestion des files et des échanges,
- Bonnes pratiques pour maximiser la fiabilité des messages.

Suivre : [clients/src/main/java/com/examples/README.md](./src/main/java/com/examples/README.md)

### Tutoriels RabbitMQ :

- Comprendre les concepts clés (exchange, queue, binding...),

Suivre : [clients/src/main/java/com/tutorials/README.md](./src/main/java/com/tutorials/README.md)

## Contribution

Les contributions sont les bienvenues ! Vous pouvez proposer des améliorations via des pull requests ou signaler des problèmes dans la section Issues.
