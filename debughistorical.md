# Journal des incidents DoubleFMediaFab

## 2026-11-21 — Première implémentation
- Projet initial créé. Compilation et tests réels à observer dans GitHub Actions.
- Activer les logs par l'interface « Journaux » ; chaque anomalie possède horodatage et étape.
- Les échecs de lecture d'une photo/vidéo sont consignés et isolés pour permettre de continuer l'analyse.
- Toute correction doit préciser erreur observée, commit, build, contrôle sur téléphone.

## 2026-11-21 — Nouvelle navigation lecteur v0.2.0
- Ajout ZoomImageView (matrice + détecteur de pincement + glissement) et barre outils sous double-tap.
- File de lecture conservée en mémoire de processus pour éviter la limite Binder : si recréation du processus, repli vers le seul média ouvert.
- Vidéos : glissement et double-tap via geste tactile, boutons lecture/pause personnalisés.
- Vérification attendue : build GitHub Actions, tests Android physiques, crash/rotation et rafraîchissement MediaStore après corbeille.
