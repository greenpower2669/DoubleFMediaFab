# DoubleFMediaFab — TODO et validation

## Implémenté en source
- [x] Projet Android natif Kotlin, thème contrasté, icône propre.
- [x] Galerie photo/vidéo et recherche locale, filtres.
- [x] Lecteur vidéo Media3 et visualiseur photo intégré.
- [x] Comparaison côte à côte, bouton corbeille avec confirmation du système.
- [x] Hachage exact SHA-256 streaming pour groupes de taille identique.
- [x] dHash visuel photos et 3 images par vidéo.
- [x] ML Kit image-labeling embarqué exécuté localement sur candidats ambigus.
- [x] Cache SQLite, logs et partage volontaire des diagnostics.
- [x] Préparation pipeline GitHub Actions et Release APK.
- [x] Lecteur immersif : swipe photo/vidéo, zoom photo pincement et boutons ±, transition légère.
- [x] Double-tap pour afficher/masquer les commandes de diaporama, corbeille et playlist de miniatures.
- [x] Playlist issue du filtre courant, diaporama automatique photo (5 s) / vidéo (fin de lecture).
- [x] Demande de corbeille dans le lecteur avec validation Android et retrait local de la playlist.

## Vérifications à effectuer après build
- [x] Compiler et publier l’APK v0.1.0 en Release directe (validé le 21/11).
- [ ] Confirmer build v0.2.0 GitHub Actions vert et Release APK accessible sans ZIP.
- [ ] Installer APK sur Samsung réel, vérifier permissions Android 14–17.
- [ ] Tester affichage galerie sur 10k/50k médias, mémoire et FPS.
- [ ] Tester doublons exacts et réencodés, orientation, recadrage, vidéos longues.
- [ ] Tester corbeille système, retour galerie et compteur récupérable.
- [ ] Vérifier performances ML Kit, consommation RAM et initialisation.
- [ ] Corriger toute erreur trouvée dans GitHub Actions et documenter la correction.

## Améliorations après fonctionnement confirmé
- [ ] Tester zoom tactile / gestes sur Samsung réel, notamment vidéos et double-tap.
- [ ] Tester les transitions et playlists volumineuses, retour de suppression et orientation.
- [ ] Comparaison vidéo synchronisée entre deux lecteurs.
- [ ] Calcul incrémental de digest SHA et recherche en arrière-plan robuste.
- [ ] Annulation/pause d'analyse avec reprise durable et notifications.
- [ ] Modèle embarqué d'embeddings sémantiques compact pour réencodage/recadrage.
- [ ] Optimiser LSH et seuils sur un corpus annoté, calibrer scores.
- [ ] Écran dossiers/date et mode sélection groupée toujours confirmé.
- [ ] Signature de distribution et contrôle d'intégrité APK.
