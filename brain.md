# DoubleFMediaFab — mémoire projet
Projet Android natif Kotlin. Objectif : nettoyer photos et vidéos locales (y compris reçues depuis messageries) tout en explorant rapidement les médias, en gardant un contrôle humain sur chaque suppression.

## Architecture décidée
- Galerie native RecyclerView + Glide, chargement progressif de miniatures, recherche par nom/dossier et filtres.
- Lecteur vidéo intégré Media3 ExoPlayer et visualiseur photo.
- Copies exactes : SHA-256 après présélection par taille, lecture streaming.
- Copies visuellement proches : dHash 64 bits, vidéo sur 15/50/85 % de durée.
- IA légère locale : modèle d'étiquetage ML Kit embarqué, uniquement pour candidats ambigus. Les étiquettes sont un signal sémantique secondaire, pas un embedding garantissant l'identité visuelle.
- Cache SQLite des signatures visuelles, logs privés locaux avec export volontaire.
- Comparaison humaine avant mise à la corbeille ; confirmation système Android.
- Pipeline CI GitHub Actions, APK debug nommée explicitement et jointe à une Release.
- minSdk 30, targetSdk 35. AAB réservée à une éventuelle publication sur Play Store.

## Invariants de sécurité
- Zéro requête réseau applicative ni permission INTERNET.
- Aucune suppression silencieuse. Le système demande son autorisation.
- Afficher « gain potentiel » et distinguer identité exacte et similarité approximative.
- Les autorisations Android peuvent ne permettre qu'une sélection de photos/vidéos.
- Les logs peuvent mentionner les noms de fichiers : export seulement volontaire.

## Limites initiales à mesurer
- Temps d'analyse avec grandes médiathèques ; plafonds de comparaisons de voisinage.
- dHash et labels IA sont heuristiques, possibles faux positifs/négatifs (rotation, recadrage, vidéos remontées).
- Modèle d'étiquetage ML Kit différent d'un modèle d'embeddings. Tester une évolution lorsque cela améliore les mesures.
- Confort zoom photo et lecture côte à côte sur petit écran.
- Signature debug de CI, non destinée à publication.

## Évolution v0.2.0 — lecteur immersif
- Glissement horizontal précédent/suivant en plein écran, y compris vidéos ; pincement zoom et déplacement photo, boutons ±.
- Double-tap fait apparaître ou masque outils et playlist de miniatures.
- Diaporama : 5 secondes par photo ; vidéos lues jusqu'au bout ; transitions fondu + léger zoom.
- Playlist basée sur le filtre galerie courant sans transférer de gros tableaux par Intent. Retour corbeille rafraîchit la galerie.
- Reste à vérifier sur appareil réel : gestures, latence sur vidéos longues et survie après destruction du processus.
