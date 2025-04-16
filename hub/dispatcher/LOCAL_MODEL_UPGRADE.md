# Build local du Dispatcher avec une version de la librairie model indisponible en ligne

Par défaut, le Dispatcher embarque une version de la librairie modèle tirée du repo distant [SAMU-Hub-Modeles](https://github.com/ansforge/SAMU-Hub-Modeles).

Il existe une version par défaut de la librairie définie dans le [build.gradle](build.gradle) du Dispatcher 
```text 
def modeVersion
```
qui peut être surchargée à la volée via une propriété gradle :
```bash
gradle build -PmodelVersion=$MODEL_VERSION
```
.

En cours de développement d'une nouvelle version de la librairie, il peut être utile de tester son intégration dans le Dispatcher avant qu'elle soit publiée.

Voici la marche à suivre :

1. Builder localement l'image de la librairie modèle depuis son repo github local :
```bash
gradle -Pversion=$MODEL_VERSION publishToMavenLocal
```
2. Dans le [build.gradle](build.gradle) du Dispatcher :
- Décommenter le bloc _mavenLocal_
- Commenter le bloc _maven_ définissant le repo Samu-Hub-Santé