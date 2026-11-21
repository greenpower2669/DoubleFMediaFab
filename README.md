# 🔎 Double F Media Fab

Explorateur multimédia Android natif, détecteur de doublons photo et vidéo, lecteur intégré, comparaison et corbeille contrôlée.

## Installation Android
Le fichier APK de test est publié dans les [Releases GitHub](https://github.com/greenpower2669/DoubleFMediaFab/releases), en téléchargement direct (pas d'archive ZIP). Il doit s'appeler `DoubleFMediaFab-v0.1.0-debug.apk`.

**Attention :** cette APK est signée par la clé de développement utilisée dans GitHub Actions. Elle sert aux essais, et sa signature pourra changer lors des prochaines compilations ; sauvegarder les préférences importantes avant une réinstallation.

## Fonctionnement
1. Autoriser l'accès aux photos et aux vidéos (sur Android 14+, autoriser toutes les photos pour un audit complet, ou uniquement une sélection pour un audit limité).
2. Explorer les miniatures avec la galerie, filtres photo/vidéo, recherche sur nom et chemin.
3. Appuyer sur **Analyser** : l'application compare d'abord les octets des fichiers de même taille (SHA-256), puis des signatures visuelles compactes (dHash), et utilise un petit modèle ML Kit local pour départager certains candidats.
4. Toucher un groupe suspect pour comparer deux médias dans l'application ; un appui long ouvre un média seul. Pour envoyer un élément à la corbeille, valider dans l'écran de comparaison puis confirmer dans la boîte système Android.
5. Consulter ou partager volontairement les **Journaux** si une analyse pose problème.

## Confidentialité et performance
- Aucun accès réseau demandé, photos et vidéos analysées sur l'appareil.
- Lecture SHA-256 par morceaux, signatures 64 bits, miniatures chargées à la demande, cache SQLite.
- Trois images échantillonnées à 15 %, 50 % et 85 % de chaque vidéo.
- Modèle d'étiquetage ML Kit **inclus dans l'APK**, hors ligne. L'IA vérifie une cohérence sémantique : elle ne constitue pas encore un modèle d'embedding visuel.
- Pour préserver les ressources, les comparaisons et l'inférence IA possèdent des limites, tracées dans les logs. Certains doublons visuellement très différents ou des recadrages importants peuvent rester à vérifier manuellement.
- Le pourcentage représente une proximité heuristique, et non une probabilité statistique calibrée.
- L'estimation de gain est indicative pour les groupes visuellement proches.

## Compilation
JDK 17, Android SDK 35, Gradle 8.11.1 et Android Gradle Plugin 8.9.2.

```bash
gradle :app:assembleDebug
```

Le build automatique s'exécute via `.github/workflows/android.yml`, calcule un SHA-256 et joint l'APK comme **asset direct** à une Release GitHub.

## Suivi et corrections
Consulter [brainmap.md](brainmap.md), [todo.md](todo.md), [debughistorical.md](debughistorical.md) et [ordres-de-mission.md](ordres-de-mission.md). Le développement privilégie la mise à disposition d'une application complète, suivie de tests sur appareil réel.
