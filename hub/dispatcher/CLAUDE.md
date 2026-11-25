# Dispatcher

Composant de routage du Hub Santé : Java 21, Spring Boot, Gradle, AMQP (RabbitMQ), MongoDB. Guide local : `../README.md`.

## Commandes
- Formatage : `./gradlew spotlessApply` (CI : `spotlessCheck`).
- Tests : `./gradlew check -PmodelVersion=X` (intégration + JaCoCo). Version de modèle par défaut : 3.3.2.
- Lancement local : `gradle bootRun --args='--spring.profiles.active=local,<profil>'` (profils `local-15-15-v1|v2|v3`, `local-15-nexsis`). Voir `../README.md`, et `LOCAL_MODEL_UPGRADE.md` pour tester un modèle non publié.
- Image locale : `./gradlew jibDockerBuild -Pversion=local-dev [-PmodelVersion=X] --image=dispatcher:local-dev` (login `dhi.io` requis pour l'image de base).

## Image
- Build : `.github/workflows/dispatcher-build.yaml`, déclenché par le push d'un tag `dispatcher-*`, par `release-module.yml`, ou à la main (`workflow_dispatch`) depuis un tag `dispatcher-*` avec l'input `model_versions`. Un job par version de modèle (`model_versions`, sinon `vars.MODEL_VERSIONS`).
- Un seul tag par version, jamais déplacé vers une autre image : `ghcr.io/ansforge/dispatcher:<version>-model-<M>`. Relancer le build d'un tag réécrit ce même tag. Pas de `latest` ni de tag par commit.
- Le workflow fixe la référence d'image (`--image`) ; `gradle/jib.gradle` ne définit que l'image de base et les variables `APP_VERSION` / `APP_MODEL_VERSION` (champs `version` / `modelVersion` des logs).
- Les tâches Jib échouent sans `-Pversion`.

## Pièges
- Le comportement dépend de la version de modèle. Le CI teste chaque version listée dans la variable de dépôt GitHub `DISPATCHER_TESTS_MODEL_VERSIONS`, **non lisible en local**. En local, lancer `./gradlew check -PmodelVersion=X` pour chaque version concernée par le changement ; demander à l'humain la liste à jour.
- `GITHUB_ACTOR` / `GITHUB_TOKEN` (ou `gpr.user` / `gpr.token`) requis par Gradle pour télécharger la lib de modèles depuis GitHub Packages.
- CI : Spotless obligatoire (gère aussi le header de licence `LICENSE_HEADER.txt`), JaCoCo >= 80 % global et sur les fichiers modifiés.
- Java 21 obligatoire (versions mélangées = IntelliJ cassé).
- Ne pas commiter `application-XXX.yaml` ni `client.preferences.csv` locaux.
