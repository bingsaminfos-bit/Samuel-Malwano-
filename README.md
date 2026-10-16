# 🎬 SMCut - AI Video Short Cutter (Android)

Application Android moderne conçue avec **Kotlin**, **Jetpack Compose** et **Material Design 3**, permettant de découper automatiquement des vidéos longues en courts clips viraux (TikTok, Reels, YouTube Shorts) avec détection de hooks par IA, sous-titres animés multilingues, recadrage 9:16 automatique et notifications push Firebase Cloud Messaging (FCM).

---

## 🚀 Compilation Automatique et Téléchargement de l'APK (GitHub Actions & Releases)

Ce projet est entièrement préconfiguré pour la compilation automatique de l'**APK de débogage** (`app-debug.apk`) à chaque push sur GitHub et sa mise à disposition directe en téléchargement.

### 1. Comment exporter et pousser sur GitHub depuis Google AI Studio
1. Dans l'interface de **Google AI Studio**, cliquez sur le menu en haut à droite (ou les paramètres du projet).
2. Sélectionnez **"Push to GitHub"** (ou **"Export to GitHub"**).
3. Connectez votre compte GitHub et choisissez le dépôt de destination (public ou privé).
4. Cliquez sur **Push** : l'ensemble des fichiers (code source, Gradle wrapper, workflow GitHub Actions) est synchronisé instantanément avec GitHub.

### 2. Téléchargement direct via GitHub Releases 📦
Dès que votre code est poussé sur GitHub :
1. Rendez-vous sur votre dépôt GitHub.
2. Cliquez sur l'onglet **"Releases"** (sur le côté droit de la page d'accueil de votre repo GitHub).
3. Vous trouverez la dernière version (ex: `v1.0.0-debug`) avec le fichier **`SMCut-v1.0-debug.apk`**.
4. Cliquez sur le fichier pour le télécharger directement sur votre téléphone Android ou votre ordinateur !

### 3. Téléchargement via GitHub Actions Artifacts ⚡
Vous pouvez également récupérer l'APK généré pour chaque commit :
1. Dans votre dépôt GitHub, cliquez sur l'onglet **"Actions"**.
2. Cliquez sur la dernière exécution du workflow nommé **"🔨 Build Debug APK & Publish Release"**.
3. Descendez en bas de la page dans la section **"Artifacts"**.
4. Cliquez sur **`SMCut-v1.0-Debug-APK`** pour télécharger l'archive contenant l'APK.

### 4. Déclencher une compilation manuelle à tout moment (Workflow Dispatch)
1. Dans GitHub, allez dans l'onglet **"Actions"**.
2. Dans la barre latérale gauche, cliquez sur le workflow **"Build and Release Android Debug APK"**.
3. Cliquez sur le bouton bleu **"Run workflow"**.
4. Choisissez la branche (`main` ou `master`), saisissez éventuellement un tag (par défaut `v1.0.0-debug`), puis validez.
5. L'APK sera compilé et la release sera générée automatiquement en 2 à 3 minutes.

---

## ✂️ Fonctionnalités Clés de l'Application

### 👑 1. Découpeur Vidéo Gratuit dans le Compte Admin
- **Accès direct & permanent** : Bouton **"ADMIN"** doré accessible dans la barre supérieure et dans la barre de navigation du bas.
- **Découpage 100% Gratuit & Illimité** : Aucune restriction, aucun abonnement requis pour l'administrateur.
- **Prise en charge de tous les liens** : YouTube, TikTok, Instagram Reels, X (Twitter), ou lien direct MP4 web. Bouton dédié pour coller instantanément depuis le presse-papier.
- **Presets de test rapides** : Boutons d'exemples pré-remplis pour tester le découpage immédiatement en 1 clic.
- **Personnalisation complète** :
  - Formats d'export : Vertical 9:16 (TikTok/Reels/Shorts), Carré 1:1, Horizontal 16:9.
  - Durées : 15s, 30s, 60s, 90s par clip.
  - Résolutions : 1080p Full HD et 4K Ultra HD.
  - Sous-titres multilingues & Traduction (Français, Anglais, Espagnol, etc.).
  - Détection virale IA des punchlines et recadrage intelligent des visages (Face Tracking).
- **Actions sur chaque clip découpé** :
  - **Télécharger (Gratuit)** : Enregistrement direct et ajout à l'historique de l'application.
  - **Partager** : Export direct vers TikTok, Instagram Reels, YouTube Shorts, WhatsApp, X ou le sélecteur Android.
  - **Lecteur intégré** : Aperçu vidéo plein écran avec sous-titres dynamiques et contrôle de lecture.
  - **Copie rapide** : Titre accrocheur et hashtags viraux prêts à être collés dans vos publications.

### 🔔 2. Notifications Push FCM (Firebase Cloud Messaging)
- Alerte push envoyée automatiquement sur votre appareil dès que le traitement de découpe de la vidéo est terminé.
- Console d'envoi et de test de notifications push intégrée dans l'onglet "FCM Push" de l'Espace Admin.

### 📁 3. Historique & Téléchargements
- Section complète répertoriant tous les clips générés et téléchargés avec statut, métadonnées, horodatage et lecteur.

### 🛠️ 4. Espace Administration Complet
- **Prix** : Modification dynamique des tarifs d'abonnements mensuels, annuels et à vie.
- **Retrait** : Suivi des revenus et demandes de virement bancaire ou Mobile Money.
- **Problèmes** : Gestion et résolution des tickets de support utilisateurs.
- **Sécurité** : Changement du mot de passe de l'administrateur.
- **Console FCM** : Déclenchement de notifications système et alertes promotionnelles.

---

## 🛠️ Stack Technique & Architecture
- **Langage** : Kotlin 2.0+
- **Interface Utilisateur** : Jetpack Compose & Material 3 (Thème sombre cinématographique)
- **Persistance** : StateFlow réactif, coroutines Kotlin & Room Database
- **Notifications** : Firebase Cloud Messaging (FCM) & Android NotificationManager
- **CI/CD** : GitHub Actions (`.github/workflows/build-apk.yml`) & Gradle Wrapper 9.3.1
