### Qu’est ce que fait cette fonction ?

1. Consomme un message sur la file `dispatch` du vhost auquel le dispatcher est associé
2. setOriginalRoutingKeyHeader
3. Extrait le message Edxl du payload
    1. valide le contenu du message vis à vis du modèle de donnée
    2. deserialize le message
        1. log le contenu
4. Effectue des checks
    1. est-ce que ce message est autorisé à circuler sur ce vhost ?
    2. est-ce que le message est inhibé pour ce destinataire ?
    3. est-ce qu’un acteur de la santé est soit destinataire soit expéditeur ?
    4. est-ce que la routing key utilisée correspond bien à l’expéditeur ?
        1. Dans les cas ou l’expéditeur n’est pas en `fr.health.*`, log un warning et laisser passer.
    5. est-ce que le delivery mode est set à persistent ?
    6. est-ce que le format du distributionID est valide
        1. Uniquement si l’expéditeur est en `fr.health.*`.
5. Détermine si une conversion est nécessaire
    1. Sauvegarde le message en base si besoin
    2. Appelle le converter
    3. serialisation
        1. Log le forwarding d’un message
    4. Publication sur un vhost de transfert
6. Si non, serialisation
7. Publication sur l’exchange distribution
8. Publication de la metric de circulation des messages

### Les irritants

> Analyse révisée après relecture du flux complet (`Dispatcher.dispatch`, `MessageHandler`, `MessageUtils`, `ConversionUtils`, `ConversionHandler`, `MessagePersistencePolicy` et les tests).

**Confirmés**

- *Checks conditionnels au type de circulation* — ✅. `checkSenderConsistency`, `checkDeliveryModeIsPersistent` et `checkDistributionIDFormat` dépendent du préfixe expéditeur ; `checkDistributionIDFormat` est même gardé par un `if` inline dans `dispatch()` (`Dispatcher.java:216-220`).
  **Nuance** : le conditionnement porte sur le *préfixe expéditeur* (`fr.health.*`), pas exactement sur les 3 `RoutingType` (qui combinent expéditeur **et** destinataire). Le mapping check → stratégie n'est donc pas 1:1.
- *Responsabilités floues* — ✅, et plus marqué qu'écrit initialement. `getFwdMessageBody` (`MessageHandler.java:481-559`) sérialise + calcule un hash + log ×3 + incrémente une métrique. `sendErrorReport` (`MessageHandler.java:191-273`) est le pire offenseur : il construit l'EDXL d'erreur **+ gère sa propre conversion + publie sur le broker + log**.

**Nuancés / invalidés**

- *« Early return crado pour au final faire la même chose »* — ⚠️ Trompeur. Les deux chemins divergent réellement :
  - Conversion → `forwardedStringMessage` (pas de sérialisation, JSON forcé, **pas d'override TTL**, log minimal) → **transfer exchange** (`ConversionUtils.buildTransferExchangeName`), routing key = `receivedRoutingKey`, boucle **1→N**, et **aucun `publishMetrics`**.
  - Normal → `forwardedMessage` (sérialise selon la préférence XML du destinataire, override TTL, log complet) → **DISTRIBUTION_EXCHANGE**, routing key = queue destinataire, **1→1**, `publishMetrics`.
  - Conclusion : structurellement proches (sérialiser + publier), mais sémantiquement distincts. Unifier reste souhaitable, à condition de préserver ces écarts.
- *« Duplication de méthodes pour générer la string »* — ❌ Largement invalidé. `forwardedMessage` (entrée = `EdxlMessage`) et `forwardedStringMessage` (entrée = string déjà convertie) ne sont pas des doublons. Le vrai défaut n'est pas la duplication mais l'**incohérence** : le chemin string perd l'override TTL et le contexte destinataire dans les logs.

**Irritants additionnels remontés**

- `ConversionUtils.determineRoutingType` lève un `RuntimeException` générique (pas une `AbstractHubException`) → tombe dans le `catch (Exception)` large de `dispatch()`. De plus, il ne gère que `fr.health`/`fr.fire`, pas `fr.cisu.*` (pourtant cité dans la doc projet).
- La « stratégie de routage » **existe déjà mais éclatée en 3 endroits** : `determineRoutingType` (privé dans `ConversionUtils`), le test `conversionType == CISU_TRANSCODING` pour la persistance (`Dispatcher.java:227`), et le `startsWith(FR_HEALTH_PREFIX)` pour `checkDistributionIDFormat`. Le concept est implicite et dispersé.
- **Lacune de tests** : `resolveConversionParameters` / `determineRoutingType` ne sont pas testés unitairement (`ConversionUtilsTest` = 3 tests utilitaires seulement). La logique de routage n'est couverte qu'indirectement via les 45 tests de `DispatcherTest` (qui asservissent le comportement externe `rabbitTemplate.send`). Le filet existe, mais il faut **ajouter des tests de caractérisation sur le routage avant tout refactor réel**.
- **Dépendance d'ordre** : `DispatcherTest` vérifie via `InOrder` que persist se fait **avant** conversion → tout design doit préserver cet ordre.

**Bonnes idées du draft**

- Centraliser la variation par route derrière une interface adresse directement les irritants « checks conditionnels » et « branches/early return ».
- `MessageRoutingDTO[]` (payload + exchange + routing key) unifie les deux chemins de publication dans un seul `sendMessages` et gère élégamment le fan-out **1→N** de la conversion.
- Séparer build / send / log / metrics rétablit des responsabilités claires.

**Corrections à apporter**

1. **Checks universels hors stratégie.** `extractMessage`, `checkMessageClassNameSupported`, `checkHealthActorIsInvolved`, `inhibitMessageIfNeeded` sont universels. Tout mettre dans `strategy.checkMessageContent` force soit de la duplication, soit une classe de base. → **Template method** : universels dans `dispatch()`, spécifiques dans la stratégie.
2. **Ordre œuf/poule.** `pickRoutingStrategy` exige un sender + recipient *fiables* → l'appeler **après** `checkHealthActorIsInvolved`. La sélection doit lever une `AbstractHubException` (pas un `RuntimeException`).
3. **Réutiliser `RoutingType`.** L'enum (`SAMU_TO_SAMU` / `CISU_TO_SAMU` / `SAMU_TO_CISU`) et les resolvers (`resolveSamuToSamu` / `resolveCisuToSamu` / `resolveSamuToCisu`) existent déjà dans `ConversionUtils`. Bâtir dessus plutôt que d'inventer une taxonomie `RoutingStrategy` parallèle.
4. **Instanciation.** Stratégies = composants Spring sélectionnés par une fabrique / `Map<RoutingType, RoutingStrategy>`, déléguant la résolution des paramètres de conversion aux méthodes `ConversionUtils` existantes (churn minimal, tests préservés).
5. **Coquille.** `logForwardedMessages(messageRoutingDTO)` référence une variable inexistante → `messageList`.

### Design recommandé

Template method dans `dispatch()` + stratégie pour la seule variation par route :

```java
public void dispatch(Message message) {
    try {
        setOriginalRoutingKeyHeader(message);
        EdxlMessage edxl = messageHandler.extractMessage(message);

        // --- checks UNIVERSELS (template) ---
        checkMessageClassNameSupported(edxl, hubConfig);
        messageHandler.inhibitMessageIfNeeded(edxl);
        checkHealthActorIsInvolved(edxl);          // garantit un routage déterminable
        checkSenderConsistency(message, edxl);     // auto-conditionné en interne
        checkDeliveryModeIsPersistent(message, edxl.getDistributionID());

        // --- sélection de stratégie (réutilise RoutingType) ---
        RoutingStrategy strategy = routingStrategyFactory.pick(edxl); // throws AbstractHubException

        // --- variation par route ---
        strategy.checkSpecificContent(edxl, message);   // ex: checkDistributionIDFormat si sender health
        List<MessageRoutingDTO> routes = strategy.buildRouting(edxl, message);
        //   buildRouting encapsule : resolveConversionParameters + persistIfNeeded (AVANT conversion)
        //   + conversion 1→N + sérialisation, OU forward direct 1→1.

        for (MessageRoutingDTO r : routes) {
            rabbitTemplate.send(r.exchange(), r.routingKey(), r.amqpMessage());
            if (r.emitCirculationMetric()) {            // ISO-COMPORTEMENT : true seulement
                messageHandler.publishMetrics(edxl, r.amqpMessage()); // pour le forward direct
            }
        }
    } catch (AbstractHubException e) {
        messageHandler.handleError(e, message);
    } catch (Exception e) {
        /* inchangé : log + AmqpRejectAndDontRequeueException */
    }
}
```

`MessageRoutingDTO` :

```java
record MessageRoutingDTO(
    Message amqpMessage,          // message AMQP prêt à envoyer
    String exchange,              // DISTRIBUTION_EXCHANGE ou transfer exchange
    String routingKey,            // queue destinataire ou receivedRoutingKey
    boolean emitCirculationMetric // false sur les chemins de conversion → préserve l'actuel
) {}
```

`RoutingStrategy` (3 implémentations Spring, 1 par `RoutingType`) :

```java
interface RoutingStrategy {
    void checkSpecificContent(EdxlMessage edxl, Message message);            // checks propres à la route
    List<MessageRoutingDTO> buildRouting(EdxlMessage edxl, Message message); // persist+convert OU forward
}
```

**Points clés du design**

- **Métriques iso-comportement** : `emitCirculationMetric = true` uniquement pour le DTO de forward direct ; les DTO issus de conversion le laissent à `false` → les messages convertis restent non comptés (comportement actuel conservé).
- **Ordre persist → convert** préservé dans `buildRouting`.
- `checkSenderConsistency` / `checkDeliveryModeIsPersistent` gardent leur auto-conditionnement interne (refactor minimal) ; seul `checkDistributionIDFormat` migre explicitement dans les stratégies `SAMU_TO_*` (sender = health).
- Les resolvers de conversion restent statiques dans `ConversionUtils` ; les stratégies les orchestrent → pas de réécriture de la logique de routage.

**Améliorations optionnelles (hors iso-comportement, à acter séparément)**

- Harmoniser le log du chemin converti pour inclure le contexte destinataire (corrige la perte de contexte de `forwardedStringMessage`).
- `determineRoutingType` → lever une `AbstractHubException` dédiée et gérer le préfixe `fr.cisu.*`.
