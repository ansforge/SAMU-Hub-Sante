# Hub Santé load testing

## Description

Outil de tests de montée en charge pour la plateforme Hub Santé. Les simulations sont construites avec [Gatling](https://gatling.io/) et publient des messages via AMQP (RabbitMQ) grâce au plugin [gatling-amqp](https://github.com/galax-io/gatling-amqp-plugin).

## Scénarios

Une seule simulation Gatling, [`TmcSimulation`](src/gatling/java/loadtesting/simulations/TmcSimulation.java), lance en parallèle tous les scénarios de [`src/gatling/java/loadtesting/scenarios/`](src/gatling/java/loadtesting/scenarios/). Un nouveau scénario s'enregistre dans la liste `SCENARIOS` de `TmcSimulation`.

| Scénario | Clé | Vhost | Message | Description | Variable du nombre d'utilisateurs |
|---|---|---|---|---|---|
| `SamuGpsScenario` | `15-gps` | `15-gps_v1.3` | Géo-position | 15-GPS : mise à jour de position GPS (SAMU→SAMU) | `SAMU_GPS_SCENARIO_USER_COUNT` |
| `SamuSmurScenario` | `15-smur` | `15-smur_v1.7` | RS-EDA | 15-SMUR : création de dossier SAMU→SMUR | `SAMU_SMUR_SCENARIO_USER_COUNT` |
| `SamuSamuDirectScenario` | `15-15-direct` | `15-15_v2.1` | RS-EDA | 15-15 : transfert direct entre deux SAMUs | `SAMU_SAMU_DIRECT_SCENARIO_USER_COUNT` |
| `SamuSamuConversionScenario` | `15-15-conversion` | `15-15_v2.1` | RS-EDA | 15-15 : conversion de version entre deux SAMUs (v3→v1) | `SAMU_SAMU_CONVERSION_SCENARIO_USER_COUNT` |
| `SamuNexsisDirectScenario` | `15-18-direct` | `15-nexsis_v1.9` | RC-EDA | 15-18 : transfert direct SAMU→NexSIS | `SAMU_NEXSIS_DIRECT_SCENARIO_USER_COUNT` |
| `SamuNexsisConversionScenario` | `15-18-conversion` | `15-15_v2.1` | RS-EDA | 15-18 : conversion de version SAMU→NexSIS | `SAMU_NEXSIS_CONVERSION_SCENARIO_USER_COUNT` |
| `SamuNexsisRsScenario` | `15-18-persistance` | `15-15_v2.1` | RS-RI puis RS-SR | Persistance 15-18, SAMU→NexSIS : RS-RI (nouveau caseId) → Dispatcher persiste ; RS-SR (même caseId) → Converter lit le RS-RI en DB et convertit en RC-RI | `SAMU_NEXSIS_RS_SCENARIO_USER_COUNT` |
| `SamuNexsisRcRiScenario` | `18-15-persistance` | `15-nexsis_vactive` | RC-RI puis RC-RI | Persistance 18-15, NexSIS→SAMU : RC-RI (nouveau caseId) → Dispatcher persiste ; RC-RI (même caseId) → Converter lit en DB, calcule le diff, retourne RS-SR | `SAMU_NEXSIS_RC_RI_SCENARIO_USER_COUNT` |

Les deux scénarios de persistance (`15-18-persistance`, `18-15-persistance`) couvrent les flux où le Dispatcher déclenche la persistance MongoDB. Cela concerne les messages RC-RI et RS-RI (flux de compte-rendu d'intervention), mais **pas** les conversions CISU pures (RC-EDA ↔ RS-EDA) qui transitent sans persistance. Ils nécessitent une instance MongoDB accessible (la même que celle du Dispatcher). Chaque virtual user envoie deux messages successifs séparés par `SCENARIO_PAUSE_SECONDS` : le premier crée un nouveau cas (nouveau `caseId`), le second met à jour ce même cas et déclenche la lecture en base.

## Variables d'environnement

| Variable | Description |
|---|---|
| `KEY_PASSPHRASE` | Passphrase du certificat client mTLS |
| `CERTIFICATE_PATH` | Chemin absolu vers le certificat client mTLS (PKCS12) |
| `TRUST_STORE_PASSWORD` | Mot de passe du truststore JKS |
| `TRUST_STORE_PATH` | Chemin absolu vers le truststore JKS |
| `RABBITMQ_HOST` | Nom d'hôte RabbitMQ |
| `RABBITMQ_PORT` | Port AMQP RabbitMQ (généralement `5671` pour TLS) |
| `SCENARIO_DURATION` | Durée d'injection des scénarios en secondes (défaut : `10`) |
| `SCENARIO_PAUSE_SECONDS` | Pause entre les deux messages des scénarios de persistance, en secondes (défaut : `5`) |
| `SAMU_GPS_SCENARIO_USER_COUNT` | Utilisateurs par seconde pour `SamuGpsScenario` (défaut : `2`, `0` = scénario désactivé) |
| `SAMU_SMUR_SCENARIO_USER_COUNT` | Utilisateurs par seconde pour `SamuSmurScenario` (défaut : `2`, `0` = scénario désactivé) |
| `SAMU_SAMU_DIRECT_SCENARIO_USER_COUNT` | Utilisateurs par seconde pour `SamuSamuDirectScenario` (défaut : `2`, `0` = scénario désactivé) |
| `SAMU_SAMU_CONVERSION_SCENARIO_USER_COUNT` | Utilisateurs par seconde pour `SamuSamuConversionScenario` (défaut : `2`, `0` = scénario désactivé) |
| `SAMU_NEXSIS_DIRECT_SCENARIO_USER_COUNT` | Utilisateurs par seconde pour `SamuNexsisDirectScenario` (défaut : `2`, `0` = scénario désactivé) |
| `SAMU_NEXSIS_CONVERSION_SCENARIO_USER_COUNT` | Utilisateurs par seconde pour `SamuNexsisConversionScenario` (défaut : `2`, `0` = scénario désactivé) |
| `SAMU_NEXSIS_RS_SCENARIO_USER_COUNT` | Utilisateurs par seconde pour `SamuNexsisRsScenario` (défaut : `2`, `0` = scénario désactivé) |
| `SAMU_NEXSIS_RC_RI_SCENARIO_USER_COUNT` | Utilisateurs par seconde pour `SamuNexsisRcRiScenario` (défaut : `2`, `0` = scénario désactivé) |
| `TMC_SCENARIOS` | Clés des scénarios à lancer, séparées par des virgules (ex. `15-gps,15-18-persistance`). Défaut : tous |

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
- le nombre d'utilisateurs par scénario (variables `*_USER_COUNT`)

Note : l'exposition des variables via `.env` est gérée par la librairie [dotenv-java](https://github.com/cdimascio/dotenv-java).

### Lancement des tests

Lancer tous les scénarios en parallèle :

```bash
./gradlew runGatlingSimulations
```

Lancer un scénario ou un sous-ensemble (clés, séparées par des virgules) :

```bash
./gradlew runGatlingSimulations -Pscenarios=15-gps
./gradlew runGatlingSimulations -Pscenarios=15-gps,15-18-persistance
```

Le rapport est écrit dans `build/reports/gatling/tmcsimulation-<timestamp>/`.

> ⚠️ **Point de vigilance Gradle 9** : les tâches `gatlingRun` et `gatlingRecorder` du plugin `io.gatling.gradle` (3.13.5) sont cassées sous Gradle 9 (`gatlingRun` appelle `Project#javaexec`, `gatlingRecorder` lit la propriété `reportsDir` de l'ancienne Convention API, tous deux supprimés en 9.0). Le plugin ne peut pas être mis à jour car gatling-amqp impose Gatling 3.13.5 : utiliser `runGatlingSimulations`, y compris pour un seul scénario. Toute commande qui force l'évaluation de toutes les tâches (ex. `./gradlew tasks`) échoue également.

`runGatlingSimulations` (comme l'image Docker) exécute une seule simulation, `loadtesting.simulations.TmcSimulation`, avec un protocole AMQP par vhost. Le run échoue si une requête est KO (assertion Gatling), si une clé de `TMC_SCENARIOS` est inconnue ou si aucun scénario n'est sélectionné.

## Architecture technique

Sur Confluence sont disponibles [l'expression de besoin](https://ans-esante.atlassian.net/wiki/spaces/HUB/pages/1256292572/Expression+de+besoin) et la [stratégie technique](https://ans-esante.atlassian.net/wiki/spaces/HUB/pages/1257865233/Strat+gie+Technique) retenue pour l'implémentation des tests de montée en charge sur le Hub Santé.

Le document de stratégie technique contient notamment un ADR sur le choix de Gatling par rapport aux autres offres disponibles sur le marché.

Note : la version de Gatling utilisée ici est la `3.13.5`. Il s'agit de la version requise par le plugin [gatling-amqp](https://github.com/galax-io/gatling-amqp-plugin) en version `1.2.10`.

Le moteur TLS natif de Gatling (BoringSSL) est désactivé dans `src/gatling/resources/gatling.conf` : la librairie native est compilée pour glibc et ne peut pas être chargée sur l'image Alpine (musl), ce qui fait échouer Gatling au démarrage. Les connexions AMQP utilisent le TLS de la JDK (`TLSConf`) et ne sont pas impactées.
