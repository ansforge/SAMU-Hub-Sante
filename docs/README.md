# 📄 Documentation client

## 👋 Introduction
Bienvenue sur la documentation client du Hub Santé. Ce dossier a pour but de guider les éditeurs souhaitant 
se raccorder au Hub Santé dan leurs travaux en regroupant les ressources principales et les pointeurs pour 
naviguer le code plus facilement. 

## 📚 Architecture
- `docs/` est le point d'entrée pour les clients du Hub Santé
  - [`cookbook.md`](./cookbook.md) contient différents snippets de code utiles pour interagir avec le Hub Santé (lancer les Producer/Consumer Java, générer votre certificat, ...)
  - [`specs/`](./specs) contient le site présentant le modèle de données de façon interactive et conviviale
  - `vXX.md` contient les informations concernant la dernière version du Hub Santé (voir section ci-dessous)
- [`certs/`](../certs) contient les certificats clients pour les tests ainsi que les informations pour générer votre certificat
- [`client/`](../client) contient l'implémentation d'un client en Java 
  - Ce client Java nous permet de tester notre infrastructure et peut vous servir de base ou d'exemple pour votre implémentation  
  - [`tutorials/`](../client/src/main/java/com/tutorials) contient les informations et le code pour faire tourner les [tutoriels](https://www.rabbitmq.com/getstarted.html) Java proposés par RabbitMQ
- [`hub/`](../hub) contient l'implémentation du Hub Santé (RabbitMQ et Dispatcher)
- [`models/`](../models) contient les spécifications AsyncAPI ainsi que les classes Java associées

## 🕰️ Versions
La liste des versions est disponible en ligne sur [GitHub](https://github.com/ansforge/SAMU-Hub-Sante/releases).

GitHub permet également de se placer directement dans le code correspondant à la version et de suivre les travaux incrémentalement :
- [v0.7](https://github.com/ansforge/SAMU-Hub-Sante/blob/0.7/docs/v0.7.md) : Migration Cloud et gestion JSON/XML
- [v0.6](https://github.com/ansforge/SAMU-Hub-Sante/blob/0.6/docs/v0.6.md) : Transcodage et validation, authentification par certificat
- [v0.5](https://github.com/ansforge/SAMU-Hub-Sante/blob/0.5/docs/v0.5.md) : Hub en Spring Boot & messages CISU
- [v0.4](https://github.com/ansforge/SAMU-Hub-Sante/blob/0.4/docs/v0.4.md) : Transcodage et spécifications AsyncAPI
- [v0.3](https://github.com/ansforge/SAMU-Hub-Sante/blob/0.3/docs/v0.3.md) : Sécurisation par certificats et mTLS
- [v0.2](https://github.com/ansforge/SAMU-Hub-Sante/blob/0.2/docs/v0.2.md) : Accusés de bonne réception
- [v0.1](https://github.com/ansforge/SAMU-Hub-Sante/blob/0.1/docs/v0.1.md) : Messages vides et acquittements techniques

Cela peut également être réalisé localement avec la commande `git checkout X.X` en remplaçant X.X par le numéro de version (0.7 par exemple).
