# Hub Santé load testing

## Description

Outil de tests de montée en charge pour la plateforme Hub Santé. Les simulations sont construites avec [Gatling](https://gatling.io/) et publient des messages via AMQP (RabbitMQ) grâce au plugin [gatling-amqp](https://github.com/galax-io/gatling-amqp-plugin).

## Simulations

Toutes les simulations se trouvent dans [`src/gatling/java/loadtesting/simulations/`](src/gatling/java/loadtesting/simulations/).

### Simulations existantes

| Simulation | Périmètre | Vhost | Message | Description |
|---|---|---|---|---|
| `SamuGpsSimulation` | 15-GPS | `15-gps_v1.3` | Géo-position | Mise à jour de position GPS (SAMU→SAMU) |
| `SamuSmurSimulation` | 15-SMUR | `15-smur_v1.7` | RS-EDA | Création de dossier SAMU→SMUR |
| `SamuSamuDirectSimulation` | 15-15 | `15-15_v2.1` | RS-EDA | Transfert direct entre deux SAMUs |
| `SamuSamuConversionSimulation` | 15-15 | `15-15_v2.1` | RS-EDA | Conversion de version entre deux SAMUs (v3→v1) |
| `SamuNexsisDirectSimulation` | 15-18 | `15-nexsis_v1.9` | RC-EDA | Transfert direct SAMU→NexSIS |
| `SamuNexsisConversionSimulation` | 15-18 | `15-15_v2.1` | RS-EDA | Conversion de version SAMU→NexSIS |

### Simulations de persistance MongoDB (périmètre 15-18)

Ces simulations couvrent les flux où le Dispatcher déclenche la persistance MongoDB. Cela concerne les messages RC-RI et RS-RI (flux de compte-rendu d'intervention), mais **pas** les conversions CISU pures (RC-EDA ↔ RS-EDA) qui transitent sans persistance. Elles nécessitent une instance MongoDB accessible (la même que celle du Dispatcher).

Chaque virtual user envoie deux messages successifs avec une pause intermédiaire : le premier crée un nouveau cas (nouveau `caseId`), le second met à jour ce même cas et déclenche la lecture en base.

| Simulation | Vhost | Direction | 1er message | 2e message (après pause) |
|---|---|---|---|---|
| `SamuNexsisRsSimulation` | `15-15_v2.1` | SAMU→NexSIS | RS-RI (nouveau caseId) → Dispatcher persiste | RS-SR (même caseId) → Converter lit le RS-RI en DB et convertit en RC-RI |
| `SamuNexsisRcRiSimulation` | `15-nexsis_vactive` | NexSIS→SAMU | RC-RI (nouveau caseId) → Dispatcher persiste | RC-RI (même caseId) → Converter lit en DB, calcule le diff, retourne RS-SR |

## Variables d'environnement

| Variable | Description |
|---|---|
| `KEY_PASSPHRASE` | Passphrase du certificat client mTLS |
| `CERTIFICATE_PATH` | Chemin absolu vers le certificat client mTLS (PKCS12) |
| `TRUST_STORE_PASSWORD` | Mot de passe du truststore JKS |
| `TRUST_STORE_PATH` | Chemin absolu vers le truststore JKS |
| `RABBITMQ_HOST` | Nom d'hôte RabbitMQ |
| `RABBITMQ_PORT` | Port AMQP RabbitMQ (généralement `5671` pour TLS) |
| `SCENARIO_DURATION` | Durée de chaque simulation en secondes (défaut : `10`) |
| `SAMU_GPS_SCENARIO_USER_COUNT` | Utilisateurs simultanés pour `SamuGpsSimulation` (défaut : `2`) |
| `SAMU_SMUR_SCENARIO_USER_COUNT` | Utilisateurs simultanés pour `SamuSmurSimulation` (défaut : `2`) |
| `SAMU_SAMU_DIRECT_SCENARIO_USER_COUNT` | Utilisateurs simultanés pour `SamuSamuDirectSimulation` (défaut : `2`) |
| `SAMU_SAMU_CONVERSION_SCENARIO_USER_COUNT` | Utilisateurs simultanés pour `SamuSamuConversionSimulation` (défaut : `2`) |
| `SAMU_NEXSIS_DIRECT_SCENARIO_USER_COUNT` | Utilisateurs simultanés pour `SamuNexsisDirectSimulation` (défaut : `2`) |
| `SAMU_NEXSIS_CONVERSION_SCENARIO_USER_COUNT` | Utilisateurs simultanés pour `SamuNexsisConversionSimulation` (défaut : `2`) |
| `SAMU_NEXSIS_RS_SCENARIO_USER_COUNT` | Utilisateurs simultanés pour `SamuNexsisRsSimulation` (défaut : `2`) |
| `SAMU_NEXSIS_RC_RI_SCENARIO_USER_COUNT` | Utilisateurs simultanés pour `SamuNexsisRcRiSimulation` (défaut : `2`) |

Variables du lanceur `TmcLauncher` (utilisé par `runGatlingSimulations` et par l'image Docker) :

| Variable | Description |
|---|---|
| `TMC_SIMULATIONS` | Simulations à lancer, séparées par des virgules (nom simple ou complet). Défaut : toutes |
| `TMC_CHILD_JVM_OPTS` | Options JVM de chaque simulation (défaut : `-Xmx512m`) |
| `TMC_RESULTS_DIR` | Répertoire des rapports Gatling (défaut : `${java.io.tmpdir}/tmc-results`, `build/reports/gatling` via Gradle) |
| `TMC_TIMEOUT_MINUTES` | Durée maximale du run ; au-delà les simulations sont arrêtées et le run échoue (défaut : `120`) |
## Utilisation en local

### Prérequis

- Java 21
- Droits de connexion :
  - Disposer du certificat client associé à l'utilisateur des tmc
  - Générer un trustStore avec la root CA IGC Santé (TEST/PROD en fonction de l'environnement visé)

### Environnement

Dans `tools/tmc`, dupliquer le fichier `.env.template` en `.env` :

```bash
cp .env.template .env
```

Remplir le fichier `.env` avec :
- les paths **absolus** du certificat et du truststore (`CERTIFICATE_PATH`, `TRUST_STORE_PATH`)
- les mots de passe associés (`KEY_PASSPHRASE`, `TRUST_STORE_PASSWORD`)
- le host et port RabbitMQ (`RABBITMQ_HOST`, `RABBITMQ_PORT`)
- la durée des scénarios en secondes (`SCENARIO_DURATION`)
- le nombre d'utilisateurs par simulation (variables `*_USER_COUNT`)

Note : l'exposition des variables via `.env` est gérée par la librairie [dotenv-java](https://github.com/cdimascio/dotenv-java).

### Lancement des tests

Lancer toutes les simulations en parallèle :

```bash
./gradlew runGatlingSimulations
```

Lancer une simulation individuelle ou un sous-ensemble (noms simples ou complets, séparés par des virgules) :

```bash
./gradlew runGatlingSimulations -Psimulations=SamuGpsSimulation
./gradlew runGatlingSimulations -Psimulations=SamuGpsSimulation,SamuSmurSimulation
```

> ⚠️ **Point de vigilance Gradle 9** : les tâches `gatlingRun` et `gatlingRecorder` du plugin `io.gatling.gradle` (3.13.5) sont cassées sous Gradle 9 (`gatlingRun` appelle `Project#javaexec`, `gatlingRecorder` lit la propriété `reportsDir` de l'ancienne Convention API, tous deux supprimés en 9.0). Le plugin ne peut pas être mis à jour car gatling-amqp impose Gatling 3.13.5 : utiliser `runGatlingSimulations`, y compris pour une seule simulation. Toute commande qui force l'évaluation de toutes les tâches (ex. `./gradlew tasks`) échoue également.

`runGatlingSimulations` exécute `loadtesting.launcher.TmcLauncher`, qui lance chaque simulation dans sa propre JVM (le plugin gatling-amqp ne gère pas plusieurs connexions RabbitMQ dans une même JVM). La liste des simulations est générée au build (`generateSimulationList`, à partir des fichiers `**/simulations/*Simulation.java`). Le run échoue si une simulation échoue (y compris une erreur de configuration) ou dépasse `TMC_TIMEOUT_MINUTES`.

## Image Docker

L'image (`./gradlew jib`) est construite par Jib à partir de `eclipse-temurin:21-jre-alpine` et contient l'application compilée (`/app/libs`), sans sources ni Gradle : son entrypoint lance directement `TmcLauncher`. Elle n'a donc pas besoin de réseau vers Maven ni de `GITHUB_TOKEN` à l'exécution (le token reste nécessaire au build pour récupérer `com.hubsante:models`).

Contrat attendu côté CronJob Kubernetes :
- ne pas surcharger `command` / `args` ;
- `/tmp` accessible en écriture (ex. `emptyDir`) si le système de fichiers racine est en lecture seule : les rapports y sont écrits et perdus en fin de pod ;
- monter le certificat et le truststore et fournir les variables d'environnement ci-dessus ;
- l'image tourne avec l'utilisateur non privilégié `1000:1000` : le certificat et le truststore montés doivent lui être lisibles (ex. `defaultMode: 0444` ou `fsGroup: 1000`) ;
- `GRADLE_USER_HOME`, `TMC_WORK_DIR` et `GITHUB_TOKEN` ne sont plus utilisés ;
- mémoire : prévoir environ 8 × le heap des simulations (`TMC_CHILD_JVM_OPTS`) + l'overhead de chaque JVM + 128 Mo pour le lanceur, soit ~5 à 6 Gi avec les valeurs par défaut.

Tester l'image en local dans des conditions proches du cluster (système de fichiers en lecture seule, utilisateur non privilégié, sans capabilities) :

```bash
./gradlew jibDockerBuild -Pimage=tmc -Pversion=local
docker run --rm --read-only --tmpfs /tmp --cap-drop ALL --security-opt no-new-privileges \
  --env-file .env \
  -e CERTIFICATE_PATH=/certs/client.p12 -e TRUST_STORE_PATH=/certs/truststore \
  -v <chemin_certificat>:/certs/client.p12:ro -v <chemin_truststore>:/certs/truststore:ro \
  -e TMC_SIMULATIONS=SamuGpsSimulation \
  tmc:local
```

## Architecture technique

Sur Confluence sont disponibles [l'expression de besoin](https://ans-esante.atlassian.net/wiki/spaces/HUB/pages/1256292572/Expression+de+besoin) et la [stratégie technique](https://ans-esante.atlassian.net/wiki/spaces/HUB/pages/1257865233/Strat+gie+Technique) retenue pour l'implémentation des tests de montée en charge sur le Hub Santé.

Le document de stratégie technique contient notamment un ADR sur le choix de Gatling par rapport aux autres offres disponibles sur le marché.

Note : la version de Gatling utilisée ici est la `3.13.5`. Il s'agit de la version requise par le plugin [gatling-amqp](https://github.com/galax-io/gatling-amqp-plugin) en version `1.2.10`.

Le moteur TLS natif de Gatling (BoringSSL) est désactivé dans `src/gatling/resources/gatling.conf` : la librairie native est compilée pour glibc et ne peut pas être chargée sur l'image Alpine (musl), ce qui fait échouer Gatling au démarrage. Les connexions AMQP utilisent le TLS de la JDK (`TLSConf`) et ne sont pas impactées.
