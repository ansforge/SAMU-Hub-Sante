# Dispatcher

Composant de routage du Hub Santé : Java 21, Spring Boot, Gradle, AMQP (RabbitMQ), MongoDB. Guide local : `../README.md`.

## Commandes
- Formatage : `./gradlew spotlessApply` (CI : `spotlessCheck`).
- Tests : `./gradlew check -PmodelVersion=X` (intégration + JaCoCo). Version de modèle par défaut : 3.3.2.
- Lancement local : `gradle bootRun --args='--spring.profiles.active=local,<profil>'` (profils `local-15-15-v1|v2|v3`, `local-15-nexsis`). Voir `../README.md`, et `LOCAL_MODEL_UPGRADE.md` pour tester un modèle non publié.

## Pièges
- Le comportement dépend de la version de modèle. Le CI teste chaque version listée dans la variable de dépôt GitHub `DISPATCHER_TESTS_MODEL_VERSIONS`, **non lisible en local**. En local, lancer `./gradlew check -PmodelVersion=X` pour chaque version concernée par le changement ; demander à l'humain la liste à jour.
- `GITHUB_ACTOR` / `GITHUB_TOKEN` requis par Gradle, même pour les tests.
- CI : Spotless obligatoire (gère aussi le header de licence `LICENSE_HEADER.txt`), JaCoCo >= 80 % global et sur les fichiers modifiés.
- Java 21 obligatoire (versions mélangées = IntelliJ cassé).
- Ne pas commiter `application-XXX.yaml` ni `client.preferences.csv` locaux.
