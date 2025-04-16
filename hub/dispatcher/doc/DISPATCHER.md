# DISPATCHER - REGLES DE GESTION

La présente note liste les différentes étapes et contrôles réalisés par le Dispatcher, entre le dépilement d'un message depuis la file d'entrée ("_**dispatch**_) et la publication sur l'exchange de sortie (_**distribution**_).

-[ ] vérification de la présence du Content-type
  - Le Content-type ("**application/json**" ou "**application/xml**") est requis, car on s'appuie dessus pour appeler les méthodes de validation et de désérialisation adéquates.
  - En cas d'absence, un message d'erreur est renvoyé
-[ ] validation du message contre les schemas
  - Cette validation est fonction du Content-type fourni
-[ ] à défaut, validation de l'enveloppe uniquement
  - Si l'enveloppe est valide, on désérialise l'enveloppe uniquement, pour en extraire avec certitude le distributionID
  - On envoie alors un message d'erreur avec distributionID et liste des erreurs de validation
  - Si l'enveloppe est invalide, on extrait, avec moins de certitude, le distributionID, en parsant la string du message
  - On envoie également un message d'erreur avec distributionID probable et liste des erreurs de validation
-[ ] désérialisation du message
  - On désérialise en fonction du Content-type
  - à ce stade, _théoriquement_, une erreur de désérialisation relèverait d'un défaut d'implémentation côté Hub, dans la mesure où les messages ont été validés.
-[ ] checkHealthActorIsInvolved
  - On vérifie que soit l'expéditeur soit le destinataire est un client du Hub (= leur client_id est préfixé en "fr.health"), de façon à se prémunir de l'usage du Hub par un Hubex hors échanges inter-hubs
-[ ] convertIncomingCisu
  - conversion RC-EDA/RS-EDA si nécessaire
-[ ] checkSenderConsistency
  - uniquement si le sender est sur le périmètre Santé (routing key préfixée en "fr.health")
  - concordance entre la routing key présentée et le champ senderID de l'EDXL
-[ ] checkDeliveryPersistent (uniquement sur périmètre health)
  - uniquement si le sender est sur le périmètre Santé
  - vérification que le delivery_mode est spécifié à "persistent"
-[ ] checkDistributionIDFormat
  - uniquement si le sender est sur le périmètre Santé
  - contrôle de format du distributionID
-[ ] fwdMessage (-> overrideExpirationIfNeeded)
  - sérialisation du message dans le langage cible du destinataire (xml ou json)
  - surcharge du time-to-live si nécessaire en fonction du champ expiration dateTimeExpires
-[ ] getRecipientQueueName
  - construction de la routing key de sortie par concaténation destinataire + type de message
-[ ] send