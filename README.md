<h1 align="center">Hub Santé</h1>
<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/version-0.0.2-blue.svg?cacheSeconds=2592000" />
  <a href="#" target="_blank">
    <img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-yellow.svg" />
  </a>
</p>

> Plateforme d'échanges de messages asynchrones entre les acteurs de l'urgence

🏠 [Page d'accueil](https://github.com/ansforge/SAMU-Hub-Sante)

## Usage

La commande `docker-compose up -d rabbitmq` permet de lancer un Container RabbitMQ localement. L'interface d'administration est accessible par http://localhost:15672.
Les commandes suivantes permettent d'interagir avec le Hub Santé local :
```
# Activate Hub Santé dispatcher
gradle -Pmain=com.hubsante.Dispatcher run

# Send messages
CLIENT_ID=Self-Sante; gradle -Pmain=com.hubsante.Send run --args "$CLIENT_ID.out.message {'to': '$CLIENT_ID', 'content': 'test'}"     
```
Les commandes suivantes permettent de construire et utiliser l'image Docker du Hub Santé localement sur un Mac M1 (les flags `platform` peuvent être supprimés pour les puces non Apple Silicon | Ref.: https://docs.docker.com/desktop/troubleshoot/known-issues/)
```
docker buildx build --platform linux/x86_64 -t dispatcher .
docker run --platform linux/x86_64 --network="host" dispatcher
```


Le dossier Java [`tutorials/`](./src/main/java/com/tutorials) contient les informations et le code pour faire tourner les [tutoriels](https://www.rabbitmq.com/getstarted.html) proposés par RabbitMQ.

Le dossier [`python/`](./python) contient les informations et le code pour faire tourner un Hub RabbitMQ localement avec un producteur et un consommateur.

## Auteur

👤 **ANS > Equipe Hub Santé**

* Site web : https://esante.gouv.fr/si-samu

## 🤝 Contribuer

Les contributions, *issues* & *pull requests* sont les bienvenues !
<br />N'hésitez pas à utiliser notre [page d'*issues*](https://github.com/ansforge/SAMU-interface-LRM/issues).

## Montrez votre support

Mettez une ⭐️ si ce projet vous a aidé !

***
_Ce README est inspiré de [readme-md-generator](https://github.com/kefranabg/readme-md-generator)_
