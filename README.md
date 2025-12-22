<h1 align="center">Hub Santé</h1>
<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/version-0.7-blue.svg?cacheSeconds=2592000" />
  <a href="#" target="_blank">
    <img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-yellow.svg" />
  </a>
</p>

> Plateforme d'échanges de messages asynchrones entre les acteurs de l'urgence

🏠 [Page d'accueil](https://github.com/ansforge/SAMU-Hub-Sante)

## Usage

Vous êtes un éditeur et vous souhaitez vous raccorder au Hub Santé -> rendez-vous sur [votre page dédiée](https://hub.esante.gouv.fr/pages/accompagnement.html)

Vous êtes un développeur et vous voulez contribuez ou reproduire le Hub Santé -> rendez-vous sur la page développeur [`hub/`](hub/README.md)

## 📚 Architecture
- [`clients/`](clients) contient l'implémentation de clients en Java, en consommation et en publication.
    - Ce client Java nous permet de tester notre infrastructure et peut vous servir de base ou d'exemple pour votre implémentation
- [`hub/`](hub) contient l'implémentation du composant de routage du Hub Santé (Dispatcher)
- [`web/`](web) contient le client et le serveur formant le [LRM de test](https://bac-a-sable.hub.esante.gouv.fr/lrm/)

## Auteur

👤 **ANS > Equipe Hub Santé**

* Site web : https://hub.esante.gouv.fr

## 🤝 Contribuer

Les contributions, *issues* & *pull requests* sont les bienvenues !
<br />N'hésitez pas à utiliser notre [page d'*issues*](https://github.com/ansforge/SAMU-Hub-Sante/issue).

## Environnement de développement local

Un fichier docker-compose est disponible dans le dossier `tools/local-dev` du projet pour démarrer les différents services nécessaires au fonctionnement du Hub Santé en local (RabbitMQ, converter, ...).
Lancez la commande suivante depuis ce dossier :

```bash
docker compose up
```

Après l'avoir lancé, pour se connecter à l'IHM de RabbitMQ, rendez-vous sur [http://localhost:15672](http://localhost:15672) avec les identifiants suivants : admin / admin.

Après avoir dupliqué le fichier .envrc.template dans un fichier .envrc, les images des applications peuvent être être choisies les y renseignant dans le fichier.

## Montrez votre soutien

Mettez une ⭐️ si ce projet vous a aidé !

***
_Ce README est inspiré de [readme-md-generator](https://github.com/kefranabg/readme-md-generator)_
