# Hub Santé load testing

## Description

TODO :

- [] Lister les scénarios implémentés et avec quelle charge

## Utilisation en local

### Prérequis

- Java 21
- Droits de connexion :
  - Disposer du certificat client associé à l'utilisateur des tmc
  - Génerer un trustStore avec la root CA IGC Santé (TEST/PROD en fonction de l'environnement visé)

### Environnement

- Dans `tools/tmc`, uppliquer le fichier `.env.template` en le renommant `.env` :

```bash
cp .env.template .env
```

- Remplir le fichier `.env` en référençant
  - les paths **absolus** du certificat et du truststore (respectivement `CERTIFICATE_PATH` et `TRUST_STORE_PASSWORD`)
  - les mots de passe associés (respectivement `KEY_PASSPHRASE` et `TRUST_STORE_PASSWORD`)
  - le host du server RabbitMQ visé (`RABBITMQ_HOST`)
  - le port du server RabbitMQ visé (`RABBITMQ_PORT`)
  - le nom de l'exchange visé pour la publication des messages sur le server RabbitMQ (`EXCHANGE_NAME`)

Note : l'exposition des variables d'environnement via fichier `.env` est géré en dev local par la librairie [dotenv-java](https://github.com/cdimascio/dotenv-java).

### Lancement des tests

Pour lancer toutes les simulations présentes dans le dossier [simulations](tools/tmc/src/gatling/java/loadtesting/simulations), utiliser la commande :

```bash
./gradlew gatlingRunAllParallel
```

Pour lancer une simulation , utiliser la commande :

```bash
./gradlew gatlingRun --simulation loadtesting.simulations.<NOM_DE_LA_SIMULATION>
```

## Architecture technique

Sur Confluence sont disponibles [l'expression de besoin](https://ans-esante.atlassian.net/wiki/spaces/HUB/pages/1256292572/Expression+de+besoin) et la [stratégie technique](https://ans-esante.atlassian.net/wiki/spaces/HUB/pages/1257865233/Strat+gie+Technique) retenue pour l'implémentation des tests de montée en charge sur le Hub Santé.

Le document de stratégie technique contient notamment un ADR sur le choix de Gatling par rapport aux autres offres disponibles sur le marché.

Note : la version de Gatling utilisée ici est la `3.11.3`. Il en est ainsi car il s'agit la version la plus récente utilisable avec le plugin [gatling-amqp](https://github.com/galax-io/gatling-amqp-plugin).

## Archive : Gatling default readme

Gatling plugin for Gradle - Java demo project

=============================================

A simple showcase of a Gradle project using the Gatling plugin for Gradle. Refer to the plugin documentation
[on the Gatling website](https://gatling.io/docs/current/extensions/gradle_plugin/) for usage.

This project includes:

- Gradle Wrapper, so you don't need to install Gradle (a JDK must be installed and $JAVA_HOME configured)
- minimal `build.gradle` leveraging Gradle wrapper
- latest version of `io.gatling.gradle` plugin applied
- sample [Simulation](https://gatling.io/docs/gatling/reference/current/general/concepts/#simulation) class,
demonstrating sufficient Gatling functionality
- proper source file layout
