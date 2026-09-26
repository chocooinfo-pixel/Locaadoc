# Carton Locator V2.1

Version préparée pour compilation depuis un téléphone avec GitHub Actions.

## Obtenir l'APK depuis Android, sans PC

1. Créer/connecter un compte GitHub.
2. Créer un nouveau dépôt, par exemple `CartonLocator`.
3. Décompresser ce projet et envoyer **tout le contenu du dossier CartonLocatorV2_1** dans le dépôt.
4. Ouvrir l'onglet **Actions**.
5. Choisir **Build CartonLocator APK**.
6. Appuyer sur **Run workflow** puis **Run workflow**.
7. Attendre la fin avec une coche verte.
8. Ouvrir l'exécution terminée.
9. En bas, dans **Artifacts**, ouvrir/télécharger **CartonLocator-APK**.
10. Décompresser l'archive téléchargée et installer `CartonLocator.apk`.

Le workflow utilise Java 17 et Gradle 8.9. Il produit un APK debug signé automatiquement, adapté au test et à l'installation directe sur Android.

## Important

L'APK debug est destiné au test interne. Pour une distribution professionnelle/Google Play, il faudra ensuite configurer une signature release avec une clé privée.
