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

Ces simulations couvrent les chemins où `isCisuConversion = true`, ce qui déclenche la persistance MongoDB dans le Dispatcher et les lectures en base dans le Converter. Elles nécessitent une instance MongoDB accessible (la même que celle du Dispatcher).

Chaque simulation s'exécute en deux phases : un **warmup** qui alimente MongoDB avec le pool de caseIds, suivi d'une **phase de charge** qui déclenche les lectures en base.

| Simulation | Vhost | Direction | Warmup | Phase de charge |
|---|---|---|---|---|
| `SamuNexsisRsSimulation` | `15-15_v2.1` | SAMU→NexSIS | RS-RI (un par caseId) → Dispatcher persiste ; Converter convertit en RC-RI | RS-SR round-robin → Converter lit le RS-RI en DB et convertit en RC-RI |
| `SamuNexsisRcRiSimulation` | `15-nexsis_v1.9` | NexSIS→SAMU | RC-RI (un par caseId, DB vide) → Dispatcher persiste ; Converter retourne RS-RI + N×RS-SR | RC-RI round-robin (caseId connu) → Converter lit en DB, diff, retourne RS-SR uniquement |

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
| `SAMU_NEXSIS_RC_RI_SCENARIO_USER_COUNT` | Utilisateurs simultanés pour `SamuNexsisRcRiSimulation` (défaut : `2`) |## Utilisation en local

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
./gradlew gatlingRunAllParallel
```

Lancer une simulation individuelle :

```bash
./gradlew gatlingRun --simulation loadtesting.simulations.<NOM_DE_LA_SIMULATION>
```

## Architecture technique

Sur Confluence sont disponibles [l'expression de besoin](https://ans-esante.atlassian.net/wiki/spaces/HUB/pages/1256292572/Expression+de+besoin) et la [stratégie technique](https://ans-esante.atlassian.net/wiki/spaces/HUB/pages/1257865233/Strat+gie+Technique) retenue pour l'implémentation des tests de montée en charge sur le Hub Santé.

Note : la version de Gatling utilisée ici est la `3.11.3`. Il s'agit de la version la plus récente utilisable avec le plugin [gatling-amqp](https://github.com/galax-io/gatling-amqp-plugin).
