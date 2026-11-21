# Ordre de mission — DoubleFMediaFab

## Ordre de livraison immédiate, 2026-11-21
1. Fournir l'ensemble du projet source Android, y compris l'IA locale, au lieu d'une succession de maquettes.
2. Déclencher la compilation GitHub Actions ; diagnostiquer les erreurs et relancer une nouvelle validation.
3. Publier l'APK de debug installable directement comme asset d'une GitHub Release. Les artifacts ZIP d'Actions sont internes.
4. S'assurer que le package est `com.doublefmediafab`, l'APK porte le nom de l'app, et afficher un SHA-256 dans les logs.
5. Préserver les photos et vidéos d'origine. La corbeille est utilisée uniquement après consentement dans l'interface Android.
6. Conserver les problèmes et actions suivantes dans todo.md et debughistorical.md.

## Tests prioritaires
- Galerie à charge, recherche et fluidité de défilement.
- ExoPlayer sur vidéo locale, comparaison avec le fichier similaire.
- IA ML Kit sur appareil hors ligne, logs exceptions si échec.
- Faux positifs de similarité, groupes exacts versus heuristiques, capacité de récupération d'espace.
- Permissions partielles Android 14+ et rafraîchissement après corbeille.

## Distribution
Première livraison : APK debug signée automatiquement par la clé debug du builder GitHub Actions. Une signature propre et stable sera nécessaire avant mise à jour durable/production. AAB uniquement pour le Play Store.
