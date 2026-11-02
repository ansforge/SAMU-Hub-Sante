# SAMU-Hub-Sante — contexte métier

## C'est quoi
**Hub Santé** (ANS, équipe Hub Santé) : plateforme d'échanges de messages **asynchrones** entre les acteurs de l'urgence : SAMU/15, SMUR, SDIS/NexSIS (18), GPS, et éditeurs de logiciels (LRM) qui s'y raccordent.
Messages = enveloppes EDXL en JSON ou XML, dont les modèles viennent du repo frère **SAMU-Hub-Modeles** (`com.hubsante:models:<version>`, via GitHub Packages).
Doc éditeurs / specs :https://hub.esante.gouv.fr/specs/

## Fonctionnement
- Broker **RabbitMQ**, un vhost par flux et par version (ex. `15-15_v2.1`, `15-nexsis_v1.9`, `15-smur_v1.7`, `15-gps_v1.3`).
- **Dispatcher** (`hub/dispatcher/`, Spring Boot) : dépile, valide contre les schémas, convertit entre versions (dont CISU), contrôle la cohérence, route, persiste (MongoDB pour les flux RI).
- Règles de gestion détaillées (FR) : `hub/dispatcher/doc/DISPATCHER.md`.
- Seuls les clients du périmètre Santé (id préfixé `fr.health`) subissent les contrôles sender / delivery-mode / distributionID.
- Content-type obligatoire : `application/json` ou `application/xml`.

## Structure
- `hub/dispatcher/` : routage (Java 21, Gradle). Guide local : `hub/README.md`.
- `clients/` : SDK Java + tutoriels de raccordement.
- `web/lrm/` : LRM de test (Nuxt/Vue + Node/TS), `web/landing/` : site vitrine.
- `tools/` : `specs` (site des specs), `annuaire`, `healthcheck`, `chatbot`, `tmc` (charge Gatling), `tnr` (non-régression bout en bout).

## Commandes
- Dispatcher : `./gradlew spotlessApply`, `./gradlew test -PmodelVersion=X`, `./gradlew check` (intégration + JaCoCo). Version de modèle par défaut : 3.3.2.
- Lancement local : `gradle bootRun --args='--spring.profiles.active=local,<profil>'` (voir `hub/README.md`, `LOCAL_MODEL_UPGRADE.md` pour un modèle non publié).
- LRM client : `npm ci`, `npm run lint`, `npm run test:e2e`. LRM server : `npm run lint`, `check-types`, `test`.
- Specs client : `pnpm install --frozen-lockfile`, `pnpm exec tsc --noEmit`, `pnpm build`.
- Python (specs server, annuaire, healthcheck) : `uv sync`, `uv run ruff format --check .`, `uv run ruff check .`, `uv run mypy .`, `uv run pytest -q`.
- Hooks : `lefthook install`.

## Pièges
- Le comportement du Dispatcher dépend de la version de modèle : tester sur **chaque version supportée** (variable CI `DISPATCHER_TESTS_MODEL_VERSIONS`).
- `GITHUB_ACTOR` / `GITHUB_TOKEN` requis par Gradle, même pour les tests.
- CI : Spotless obligatoire, JaCoCo >= 80 % global et sur les fichiers modifiés.
- Ne jamais commiter `.env`, certificats, `application-XXX.yaml`.
- Plusieurs gestionnaires de paquets : npm (LRM), pnpm (specs client), uv (Python), Gradle (Java).
- Commits en Conventional Commits avec scope (`feat(specs/client): ...`).
- `hub/infra/` est cité dans le README mais absent de l'arbre.

## Workflow obligatoire
Pour toute tâche non triviale (feature, bugfix non trivial, refactor, évolution de modèle) :

0. **Confirmer le flow** : en début de tâche, demander à l'humain s'il veut bien suivre ce flow (ou une variante). Ne pas supposer.
1. **Conception** (skill `design`) : why, definition of done vérifiable, stratégie technique, plan de tests. Attendre la validation de l'humain. Aucun code avant.
2. **Implémentation + tests en TDD** (skill `tdd-implement`, agent `implementer`). Tests d'abord, couverture maximale.
3. **Revue + validation de la DoD** (skill `review-dod`, agent `reviewer`), par un autre agent ou membre de l'équipe que l'implémenteur.
4. **PR** (skill `prepare-pr`) : présenter commits et PR à l'humain avant d'agir.

Tâche triviale (typo, doc mineure) : dire qu'on saute le flow et pourquoi.

## Versioning
Jamais de commit, push, tag, merge ou PR sans accord explicite de l'humain, action par action. Un accord pour commiter n'est pas un accord pour pousser. Un garde-fou est aussi défini dans `.claude/settings.json`.
