# SAMU-Hub-Sante — contexte métier

## C'est quoi
**Hub Santé** (ANS, équipe Hub Santé) : plateforme d'échanges de messages **asynchrones** entre les acteurs de l'urgence : SAMU/15, SMUR, SDIS/NexSIS (18), GPS, et éditeurs de logiciels (LRM) qui s'y raccordent.
Messages = enveloppes EDXL en JSON ou XML, dont les modèles viennent du repo frère **SAMU-Hub-Modeles** (`com.hubsante:models:<version>`, via GitHub Packages).
Doc éditeurs / specs : https://hub.esante.gouv.fr/specs/

## Fonctionnement
- Broker **RabbitMQ**, un vhost par flux et par version (ex. `15-15_v2.1`, `15-nexsis_v1.9`, `15-smur_v1.7`, `15-gps_v1.3`).
- **Dispatcher** (`hub/dispatcher/`, Spring Boot) : dépile, valide contre les schémas, convertit entre versions (dont CISU), contrôle la cohérence, route, persiste (MongoDB pour les flux RI).
- Seuls les clients du périmètre Santé (id préfixé `fr.health`) subissent les contrôles sender / delivery-mode / distributionID.
- Content-type obligatoire : `application/json` ou `application/xml`.

## Structure
Chaque composant a son propre `CLAUDE.md` (stack, commandes, pièges), chargé quand on travaille dans son dossier.
- `hub/dispatcher/` : routage (Java 21, Gradle).
- `clients/` : SDK Java + tutoriels de raccordement.
- `web/lrm/` : LRM de test (Nuxt/Vue + Node/TS), `web/landing/` : site vitrine.
- `tools/` : `specs` (site des specs), `annuaire`, `healthcheck`, `chatbot`, `tmc` (charge Gatling), `tnr` (non-régression bout en bout).

## Transverse
- Hooks Git : `lefthook install`.
- Commits en Conventional Commits avec scope (`feat(specs/client): ...`).
- Ne jamais commiter `.env`, certificats, `application-XXX.yaml`.

## Workflow obligatoire
Pour toute tâche non triviale (feature, bugfix non trivial, refactor, évolution de modèle) :

0. **Confirmer le flow** : en début de tâche, demander à l'humain s'il veut bien suivre ce flow (ou une variante). Ne pas supposer.
1. **Conception** (skill `design`) : why, use cases et edge cases, critères d'acceptance, definition of done, stratégie technique (avec ADR), plan de tests. Attendre la validation de l'humain. Aucun code avant.
2. **Implémentation + tests en TDD** (skill `tdd-implement`, agent `implementer`). Tests d'abord, couverture maximale.
3. **Revue + validation des critères d'acceptance et de la DoD** (skill `review-dod`, agent `reviewer`), par un autre agent ou membre de l'équipe que l'implémenteur.
4. **PR** (skill `prepare-pr`) : présenter commits, PR et mode opératoire de validation à l'humain avant d'agir.

Tâche triviale (typo, doc mineure) : dire qu'on saute le flow et pourquoi.

## Versioning
Jamais de commit, push, tag, merge ou PR sans accord explicite de l'humain, action par action. Un accord pour commiter n'est pas un accord pour pousser. Un garde-fou est aussi défini dans `.claude/settings.json`.
