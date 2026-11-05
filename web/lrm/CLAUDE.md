# LRM de test

Application de test jouant le rôle d'un logiciel de régulation médicale raccordé au Hub. `client/` : Nuxt/Vue (Cypress e2e). `server/` : Node/TS. Gestionnaire de paquets : npm.

## Commandes
- Client : `npm ci`, `npm run lint` (ou `lint:js` / `lint:css`, `lintfix`), `npx prettier --check`, `npm run test:e2e`.
- Serveur : `npm run lint`, `npm run check-types`, `npm run test`.

## Pièges
- Lancer les tests e2e en local si le client est modifié (case de la PR template).
- Une release GitHub `lrm-<tag>` déclenche le build de l'image.
