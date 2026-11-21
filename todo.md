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

## Vérifications à effectuer après build
- [ ] Confirmer build GitHub Actions vert et Release APK accessible sans ZIP.
- [ ] Installer APK sur Samsung réel, vérifier permissions Android 14–17.
- [ ] Tester affichage galerie sur 10k/50k médias, mémoire et FPS.
- [ ] Tester doublons exacts et réencodés, orientation, recadrage, vidéos longues.
- [ ] Tester corbeille système, retour galerie et compteur récupérable.
- [ ] Vérifier performances ML Kit, consommation RAM et initialisation.
- [ ] Corriger toute erreur trouvée dans GitHub Actions et documenter la correction.

## Améliorations après fonctionnement confirmé
- [ ] Zoom tactile et comparaison vidéo synchronisée.
- [ ] Calcul incrémental de digest SHA et recherche en arrière-plan robuste.
- [ ] Annulation/pause d'analyse avec reprise durable et notifications.
- [ ] Modèle embarqué d'embeddings sémantiques compact pour réencodage/recadrage.
- [ ] Optimiser LSH et seuils sur un corpus annoté, calibrer scores.
- [ ] Écran dossiers/date et mode sélection groupée toujours confirmé.
- [ ] Signature de distribution et contrôle d'intégrité APK.
