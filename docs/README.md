# 📄 Documentation client

## 👋 Introduction
Bienvenue sur la documentation client du Hub Santé. Ce dossier a pour but de guider les éditeurs souhaitant 
se raccorder au Hub Santé dan leurs travaux en regroupant les ressources principales et les pointeurs pour 
naviguer le code plus facilement. 

## 📚 Architecture
- `docs/` est le point d'entrée pour les clients du Hub Santé
  - [`DST`](./DST) contient la dernière version du DST ainsi que les fichiers complémentaires (schémas, exemples, ...)
- [`certs/`](../certs) contient les certificats clients pour les tests ainsi que les informations pour générer votre certificat
- [`clients/`](../clients) contient l'implémentation de clients en Java, en consommation et en publication. 
  - Ce client Java nous permet de tester notre infrastructure et peut vous servir de base ou d'exemple pour votre implémentation
- [`hub/`](../hub) contient l'implémentation du composant de routage du Hub Santé (Dispatcher)
