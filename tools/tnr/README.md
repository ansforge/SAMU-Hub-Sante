# Tests de non régression (TNR)

## Vue d'ensemble

Les tests de non régression valident le cycle complet de bout en bout : un `Producer` publie un message EDXL sur RabbitMQ, et un `Consumer` le reçoit. Ils s'exécutent contre une **instance RabbitMQ réelle** ou **locale** (Docker), sans mock.

## Prérequis

- Docker actif avec le service RabbitMQ démarré
- Un fichier `.env` valide à la racine du projet (voir ci-dessous)
- Certificats TLS locaux

### Clés `.env` requises

| Clé | Description |
|---|---|
| `HUB_HOSTNAME` | Hôte RabbitMQ (ex. `localhost`) |
| `HUB_PORT` | Port AMQP over TLS (ex. `5671`) |
| `EXCHANGE_NAME` | Exchange AMQP |
| `CERTIFICATE_PATH` | Chemin vers le certificat client `.p12` (PKCS12) |
| `KEY_PASSPHRASE` | Mot de passe du certificat client |
| `TRUST_STORE_PATH` | Chemin vers le trust store JKS |
| `TRUST_STORE_PASSWORD` | Mot de passe du trust store |

## Lancement

```bash
# Lancer tous les tests
./gradlew test

# Lancer un test spécifique
./gradlew test --tests "tnr.AppTest.messageFromV3ToV1"

# Forcer la ré-exécution (Gradle cache les résultats si rien n'a changé)
./gradlew test --rerun-tasks

# Forcer la ré-exécution sans cache de config (utile si changements dans les vars d'env)
./gradlew test --no-configuration-cache

# Forcer la ré-exécution et rebuild sans cache de config (utile si changements dans les vars d'env)
./gradlew clean test --no-configuration-cache
```

## Arborescence

```
data/
├── edxl-v1.json              ← exemple de message EDXL v1
└── edxl-v3.json              ← exemple de message EDXL v3
src/
├── main/java/tnr/
│   ├── Constants.java        ← constantes (TLS, content-types, schémas)
│   ├── Consumer.java         ← consommateur AMQP abstrait
│   ├── Producer.java         ← producteur AMQP avec support TLS
│   ├── MessageBuilder.java   ← construction de messages EDXL
│   ├── AckBuilder.java       ← construction de messages d'acquittement (ACK)
│   ├── TLSConf.java          ← configuration SSL/TLS (PKCS12 + JKS)
│   ├── Utils.java            ← utilitaires (generateDistributionId, getReferencedDistributionID)
│   └── dto/
│       └── MessageDTO.java   ← DTO encapsulant un message reçu (vhost, queue, payload, distributionId)
└── test/java/tnr/
    ├── AMQPTestSupport.java  ← infrastructure de test partagée
    └── AppTest.java          ← cas de test
```

---

## Architecture

### `AMQPTestSupport` — infrastructure partagée

Annotée avec `@TestInstance(PER_CLASS)` : JUnit crée **une seule instance de la classe de test pour toute la suite**, ce qui permet aux méthodes `@BeforeAll` / `@AfterAll` d'accéder aux champs d'instance.

```
@BeforeAll  setUpAll()       → une connexion AMQP par Producer (3) et Consumer (4)
  │
  ├─ @BeforeEach clearInbox()   → vide le collecteur entre chaque test
  ├─ @Test ...
  ├─ @BeforeEach clearInbox()
  ├─ @Test ...
  │
@AfterAll   tearDownAll()    → ferme toutes les connexions
```

#### `TestConsumer`

Sous-classe de `Consumer` interne au package test. Chaque livraison est désérialisée en `MessageDTO` puis transmise au `MessageCollector` partagé.

```java
@Override
protected void deliverCallback(String consumerTag, Delivery delivery) throws IOException {
    consumeChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    MessageDTO message = new MessageDTO(this.vhost, this.queueName, delivery);
    inbox.add(message);
}
```

L'ACK est envoyé immédiatement pour que le message soit retiré de la queue, quel que soit le traitement côté test.

#### `MessageDTO` — encapsulation du message reçu

Le `MessageDTO` encapsule les métadonnées d'un message reçu : `vhost`, `queue`, `payload` (JSON) et `distributionId` (extrait automatiquement du payload à la construction).

#### `MessageCollector` — synchronisation asynchrone

Le `MessageCollector` est le coeur de la stratégie de synchronisation entre le thread JUnit et les threads RabbitMQ. Il utilise un `ReentrantLock`, un buffer de `MessageDTO` et des `CompletableFuture` pour les attentes en cours.

```
Thread JUnit                              Thread interne RabbitMQ
────────────────────────────────          ──────────────────────────
producer.publish(...)            ────────▶  le message arrive au broker
                                            deliverCallback() se déclenche
inbox.add(messageDTO)  ◀─────────────────  ajoute le MessageDTO au collecteur
awaitMessage() se débloque ✓
```

**Fonctionnement de `awaitMessage(distributionId, timeout, unit)` :**

1. Verrouille le lock et cherche dans le **buffer** un `MessageDTO` dont le `distributionId` correspond (comparaison exacte)
2. Si trouvé → le retire du buffer et le retourne immédiatement
3. Sinon → crée un `CompletableFuture<MessageDTO>` et l'enregistre dans la map `pending`
4. Attend avec `future.get(timeout, unit)` jusqu'à résolution ou expiration

**Fonctionnement de `add(MessageDTO)` (appelé par le `TestConsumer`) :**

1. Verrouille le lock et parcourt les `CompletableFuture` en attente
2. Si le `distributionId` du message correspond à une attente → complète la future et retourne
3. Sinon → bufferise le `MessageDTO` pour une recherche ultérieure

Ce mécanisme gère les messages arrivant dans le désordre : un message peut arriver avant que le test ne l'attende (bufferisé), ou le test peut attendre avant que le message n'arrive (future).

### `AppTest` — cas de test

Étend `AMQPTestSupport` et hérite des producteurs/consommateurs. Chaque méthode de test valide le cycle complet **message + acquittement** :

1. Lit le fichier JSON EDXL depuis le chemin configuré dans `.env`
2. Génère un `distributionId` unique via `Utils.generateDistributionId(clientId)`
3. Construit le message EDXL via `MessageBuilder`
4. Publie le message via un `Producer`
5. Appelle `awaitMessage(distributionId)` pour attendre la réception
6. Vérifie que le `MessageDTO` reçu contient le bon `vhost` et la bonne `queue`
7. Envoie un ACK via `sendAck()` depuis le destinataire vers l'expéditeur
8. Vérifie que l'ACK est reçu sur la bonne queue `.ack` avec le bon `referencedDistributionID`

#### Exemple de test

Voici un exemple simplifié illustrant le cycle message + ACK entre deux SAMU sur le même vhost :

```java
@Test
void samuAEnvoieUnMessageASamuC() throws Exception {
    String useCase = Files.readString(Path.of(dotenv.get("EDXL_EXAMPLE_FILE_PATH_V1")));

    // 1. samuA envoie un message à samuC
    String distributionId = Utils.generateDistributionId("fr.health.test.samuA");
    String edxlJson = new MessageBuilder().buildMessage(
            useCase, distributionId, "fr.health.test.samuA", "fr.health.test.samuC",
            DistributionKind.REPORT, DistributionStatus.ACTUAL);

    sendMessage("15-15_v1.5", "fr.health.test.samuA", edxlJson);

    // 2. samuC reçoit le message
    MessageDTO matched = awaitMessage(distributionId);

    assertNotNull(matched);
    assertEquals("15-15_v1.5", matched.getVhost());
    assertEquals("fr.health.test.samuC.message", matched.getQueue());

    // 3. samuC acquitte le message
    String ackDistributionId = Utils.generateDistributionId("fr.health.test.samuC");
    sendAck("15-15_v1.5", "fr.health.test.samuC", ackDistributionId, "fr.health.test.samuA");

    // 4. samuA reçoit l'acquittement
    MessageDTO matchedAck = awaitMessage(ackDistributionId);

    assertNotNull(matchedAck);
    assertEquals("15-15_v1.5", matchedAck.getVhost());
    assertEquals("fr.health.test.samuA.ack", matchedAck.getQueue());
}
```

#### Cas de test

| Test | Description |
|---|---|
| `messagePublishedByProducerIsReceivedByConsumer` | SAMU1-V1 → SAMU2-V1 + ACK retour (même version, vhost `15-15_v1.5`) |
| `messageFromV3ToV1` | SAMU-V3 → SAMU1-V1 + ACK retour (inter-versions, v3 → v1) |
| `messageFromV1ToV3` | SAMU1-V1 → SAMU-V3 + ACK retour (inter-versions, v1 → v3) |
| `messageFromV1ToNexsis` | SAMU1-V1 → SDIS-Z + ACK retour (intégration Nexsis, vhost `15-nexsis_v1.9`) |
| `messagePublishedBy2ProducersCanBeReceivedInMisorder` | 2 messages + 2 ACK, réception dans l'ordre inverse |
