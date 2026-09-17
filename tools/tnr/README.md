# Tests de non régression (TNR)

## Vue d'ensemble

Les tests de non régression valident le cycle complet de bout en bout : un `Producer` publie un message EDXL sur RabbitMQ, et un `Consumer` le reçoit. Ils s'exécutent contre une **instance RabbitMQ réelle** ou **locale** (Docker), sans mock.

Les use cases sont récupérés dynamiquement depuis GitHub (tag de version configuré dans `TestConstants`) via un `HttpClientWithCache`.

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
| `GITHUB_TOKEN` | Token GitHub pour récupérer les use cases depuis SAMU-Hub-Modeles |

## Lancement

```bash
# Lancer tous les tests
./gradlew test

# Lancer une classe de test spécifique
./gradlew test --tests "tnr.SamuSamuTest"

# Lancer un test spécifique
./gradlew test --tests "tnr.SamuFireTest.messageFromSamu1V1ToNexsis"

# Forcer la ré-exécution (Gradle cache les résultats si rien n'a changé)
./gradlew test --rerun-tasks

# Forcer la ré-exécution sans cache de config (utile si changements dans les vars d'env)
./gradlew test --no-configuration-cache

# Forcer la ré-exécution et rebuild sans cache de config
./gradlew clean test --no-configuration-cache
```

## Arborescence

```
src/
├── main/java/tnr/
│   ├── Constants.java            ← constantes techniques (TLS, content-types)
│   ├── MessageType.java          ← enum des types de messages EDXL (CREATE_CASE, RESOURCES_INFO…)
│   ├── Consumer.java             ← consommateur AMQP abstrait
│   ├── Producer.java             ← producteur AMQP avec support TLS
│   ├── MessageBuilder.java       ← construction de messages EDXL
│   ├── AckBuilder.java           ← construction de messages d'acquittement (ACK)
│   ├── HttpClientWithCache.java  ← client HTTP avec cache Caffeine (fetch des use cases GitHub)
│   ├── TLSConf.java              ← configuration SSL/TLS (PKCS12 + JKS)
│   ├── Utils.java                ← utilitaires (generateDistributionId, isMessageOfType…)
│   └── dto/
│       └── MessageDTO.java       ← DTO encapsulant un message reçu (vhost, queue, payload, distributionId)
└── test/java/tnr/
    ├── TestConstants.java        ← constantes partagées (IDs clients, vhosts, tags de version, use cases)
    ├── DistributionAssertions.java ← assertions custom (assertVhostEquals, assertQueueEquals, assertRsRi, assertRsSr)
    ├── AMQPTestSupport.java      ← infrastructure de test partagée (producers, consumers, inbox)
    ├── SamuSamuTest.java         ← scénarios SAMU↔SAMU (conversions inter-versions)
    └── SamuFireTest.java         ← scénarios SAMU↔SDIS/Nexsis (transcoding, cycle RC-RI)
```

---

## Architecture

### `AMQPTestSupport` — infrastructure partagée

Annotée avec `@TestInstance(PER_CLASS)` : JUnit crée **une seule instance de la classe de test pour toute la suite**, ce qui permet aux méthodes `@BeforeAll` / `@AfterAll` d'accéder aux champs d'instance.

```
@BeforeAll  setUpAll()       → une connexion AMQP par Producer et Consumer (selon config)
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

Encapsule les métadonnées d'un message reçu : `vhost`, `queue`, `payload` (JSON) et `distributionId` (extrait automatiquement du payload à la construction).

#### `MessageCollector` — synchronisation asynchrone

Le `MessageCollector` est le cœur de la stratégie de synchronisation entre le thread JUnit et les threads RabbitMQ. Il utilise un `ReentrantLock`, un buffer de `MessageDTO` et des `CompletableFuture` pour les attentes en cours.

```
Thread JUnit                              Thread interne RabbitMQ
────────────────────────────────          ──────────────────────────
producer.publish(...)            ────────▶  le message arrive au broker
                                            deliverCallback() se déclenche
inbox.add(messageDTO)  ◀─────────────────  ajoute le MessageDTO au collecteur
awaitMessage() se débloque ✓
```

**Fonctionnement de `awaitMessageByDistributionId(distributionId, timeout, unit)` :**

1. Verrouille le lock et cherche dans le **buffer** un `MessageDTO` dont le `distributionId` correspond (comparaison exacte)
2. Si trouvé → le retire du buffer et le retourne immédiatement
3. Sinon → crée un `CompletableFuture<MessageDTO>` et l'enregistre dans la map `pending`
4. Attend avec `future.get(timeout, unit)` jusqu'à résolution ou expiration

**Fonctionnement de `awaitMessageOfType(messageType, timeout, unit)` :**

Fonctionne de la même façon qu'`awaitMessageByDistributionId`, mais le critère de correspondance est le **type de message** (vérifié via `Utils.isMessageOfType(msg, messageType)`) plutôt que le `distributionId`. Utile pour attendre un message dont on ne contrôle pas le `distributionId` de sortie — par exemple un `RESOURCES_STATUS` généré par le hub en réponse à un RC-RI entrant.

**Fonctionnement de `add(MessageDTO)` (appelé par le `TestConsumer`) :**

1. Verrouille le lock et parcourt les `CompletableFuture` en attente
2. Si le message satisfait le prédicat d'une attente → complète la future et retourne
3. Sinon → bufferise le `MessageDTO` pour une recherche ultérieure

Ce mécanisme gère les messages arrivant dans le désordre : un message peut arriver avant que le test ne l'attende (bufferisé), ou le test peut attendre avant que le message n'arrive (future).

### `TestConstants` — constantes partagées

Centralise toutes les constantes utilisées dans les tests.

### `DistributionAssertions` — assertions custom

Assertions statiques importées via `import static`. Focalisées uniquement sur la vérification — elles ne font jamais d'attente réseau.

| Méthode | Description |
|---|---|
| `assertVhostEquals(msg, expected)` | Vérifie le vhost du message reçu |
| `assertQueueEquals(msg, expected)` | Vérifie la queue de destination |
| `assertRsRi(msg, distributionId)` | Vérifie qu'un message est un RS-RI valide sur le bon vhost/queue |
| `assertRsSr(messages, caseId, resourceIds)` | Vérifie une liste de RS-SR : type, vhost, queue, caseId, resourceIds, absence de `position` |

### Classes de test

#### `SamuSamuTest` — scénarios SAMU↔SAMU

Valide les échanges entre SAMU avec conversion de format entre versions. Chaque test suit le cycle complet :

```java
@Test
void messageFromSamu1V3ToSamu2V3() throws Exception {
    String useCase = getUseCaseContentOnline(V3_SAMU_TAG, RS_EDA_REF);

    // 1. SAMU1-V3 envoie un RS-EDA à SAMU2-V3
    String distributionId = Utils.generateDistributionId(SAMU1_V3_ID);
    String edxlJson = new MessageBuilder().buildMessage(
            useCase, distributionId, SAMU1_V3_ID, SAMU2_V3_ID);
    sendMessage(VHOST_15_15_V3_TAG, SAMU1_V3_ID, edxlJson);

    // 2. SAMU2-V3 reçoit le message
    MessageDTO matched = awaitMessageByDistributionId(distributionId);
    assertNotNull(matched, "Message " + distributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
    assertVhostEquals(matched, VHOST_15_15_V3_TAG);
    assertQueueEquals(matched, SAMU2_V3_ID + ".message");
    assertTrue(Utils.isMessageOfType(matched, MessageType.CREATE_CASE_HEALTH));

    // 3. SAMU2-V3 acquitte le message
    String ackDistributionId = sendAck(VHOST_15_15_V3_TAG, SAMU2_V3_ID, SAMU1_V3_ID, distributionId);

    // 4. SAMU1-V3 reçoit l'acquittement
    MessageDTO matchedAck = awaitMessageByDistributionId(ackDistributionId);
    assertNotNull(matchedAck, "Ack " + ackDistributionId + " not received within " + RECEIVE_TIMEOUT_SECS + "s");
    assertVhostEquals(matchedAck, VHOST_15_15_V3_TAG);
    assertQueueEquals(matchedAck, SAMU1_V3_ID + ".ack");
    assertEquals(distributionId, Utils.getReferencedDistributionID(matchedAck));
}
```

| Test | Description |
|---|---|
| `messageFromSamu1V3ToSamu2V3` | V3→V3, même vhost, sans conversion |
| `messageFromSamu1V2ToSamu2V2` | V2→V2, même vhost, sans conversion |
| `messageFromSamu1V1ToSamu2V1` | V1→V1, même vhost, sans conversion |
| `messageFromSamu1V1ToSamu1V3` | V1→V3, conversion de format |
| `messageFromSamu1V3ToSamu1V1` | V3→V1, conversion de format |

Chaque test valide le cycle complet : envoi du message → réception → envoi de l'ACK → réception de l'ACK avec `referencedDistributionID` correct.

#### `SamuFireTest` — scénarios SAMU↔SDIS/Nexsis

Valide les échanges entre SAMU et le système Nexsis (SDIS) avec transcoding :

| Test | Description |
|---|---|
| `messageFromSamu1V1ToNexsis` | SAMU V1 → Nexsis, conversion + transcoding |
| `messageFromSamu1V3ToNexsis` | SAMU V3 → Nexsis, transcoding uniquement |
| `messageFromSamu2V3ToNexsis` | SAMU V3 → Nexsis, sans conversion ni transcoding |
| `messageFromNexsisToSamu1V3` | Nexsis → SAMU V3, transcoding uniquement |
| `messageFromNexsisToSamu1V1` | Nexsis → SAMU V1, conversion + transcoding |
| `messageFromNexsisToSamu2V3` | Nexsis → SAMU V3, sans conversion ni transcoding |
| `messageRcRiRaymondeLecciaLifecycle` | Cycle complet RC-RI : 6 étapes (ajout ressources, mises à jour statuts, no-op) |

#### Cycle de vie RC-RI

Le test `messageRcRiRaymondeLecciaLifecycle` valide le traitement d'un incident incendie sur 6 étapes. Chaque étape suit le pattern :

```java
// 1. Envoi du RC-RI entrant
String stepDistId = sendRcRi(RC_RI_REF, uniqueCaseId);

// 2. Attente des messages sortants
MessageDTO rsRi = awaitMessageByDistributionId(stepDistId);
List<MessageDTO> rsSr = List.of(awaitMessageOfType(MessageType.RESOURCES_STATUS));

// 3. Assertions (pures, sans attente réseau)
assertRsRi(rsRi, stepDistId);
assertRsSr(rsSr, uniqueCaseId, Set.of(RC_RI_RESOURCE_ID));

// 4. ACK
sendAndAssertAck(stepDistId);
```
