# SmartScan Notes - Application Android OCR + Résumé IA

##  Description Générale

**SmartScan Notes** est une application Android qui combine la reconnaissance optique
de caractères (OCR) avec l'intelligence artificielle pour transformer des photographies
de documents en notes structurées et résumées.

**Auteurs :**
- Ferjani Med Amine (ferjabeast@gmail.com)
- Aziz Madhbouh (azizMadhbouh@gmail.com)

### Fonctionnalités Principales
-  **Authentification Firebase** : Connexion sécurisée par email/mot de passe
-  **Capture ou Import de Documents** : Prendre une photo ou sélectionner une image depuis la galerie
-  **Reconnaissance OCR** : Extraction de texte via Google ML Kit
-  **Résumé Intelligent** : Génération de résumés avec le modèle `facebook/bart-large-cnn` via Hugging Face
- ️ **Extraction de Mots-Clés** : Identification automatique des termes importants
-  **Stockage Local** : Base de données SQLite pour les notes
-  **Recherche** : Recherche full-text parmi les notes sauvegardées

---

##  Écrans Principaux

1. **MainActivity (Connexion)** - Écran de login avec Firebase
2. **CreateAccountActivity (Inscription)** - Créer un nouveau compte utilisateur
3. **NotesListActivity (Liste + Recherche)** - Voir toutes les notes, chercher, se déconnecter
4. **CaptureActivity (Capture + Traitement)** - Prendre photo → OCR → Résumé → Sauvegarder
5. **NoteDetailActivity (Détail)** - Afficher une note complète

---

##  Architecture des Fichiers

###  Authentification & Utilisateurs

#### `MainActivity.java`
Interface de login avec Firebase Authentication. Gère les champs email/mot de passe, lance l'inscription ou entre dans l'app.

```java
// Vérifie les identifiants Firebase et navigue vers NotesListActivity
mAuth.signInWithEmailAndPassword(email, password)
    .addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
            startActivity(new Intent(MainActivity.this, NotesListActivity.class));
        }
    });
```

#### `CreateAccountActivity.java`
Écran d'enregistrement d'un nouvel utilisateur. Valide les mots de passe et crée le compte Firebase.

###  Capture & OCR

#### `CaptureActivity.java`
Contrôle la caméra (CameraX), affiche l'aperçu en direct, capture ou importe l'image, puis lance l'OCR et le résumé IA.

**Flux :**
1. Utilisateur autorise l'accès à la caméra
2. CameraX affiche le flux en temps réel
3. L'utilisateur capture une photo ou en sélectionne une
4. ML Kit extrait le texte (OCR)
5. SummaryService génère un résumé + mots-clés
6. NotesRepository sauvegarde en SQLite

###  Résumé & IA

#### `SummaryService.java` (★ Fichier Clé)
Service qui communique avec l'API Hugging Face via Retrofit pour résumer le texte OCR.

**Points clés :**
- Endpoint : `https://router.huggingface.co/hf-inference/models/facebook/bart-large-cnn`
- Authentication : `Bearer {HUGGING_FACE_TOKEN}`
- Le token est injecté depuis `.env` via `BuildConfig.HUGGING_FACE_TOKEN`

```java
Map<String, Object> request = new HashMap<>();
request.put("inputs", normalizedText);

Map<String, Object> parameters = new HashMap<>();
parameters.put("max_length", 120);
parameters.put("min_length", 30);
parameters.put("do_sample", false);
request.put("parameters", parameters);

api.summarize("Bearer " + HF_TOKEN, request).enqueue(callback);
```

**Extraction de mots-clés :** via `buildKeywords()` qui filtre les mots vides (stopwords) et extrait les 6 premiers mots pertinents.

#### `HuggingFaceApi.java`
Interface Retrofit pour l'appel API Hugging Face.

```java
@POST("hf-inference/models/facebook/bart-large-cnn")
Call<ResponseBody> summarize(
    @Header("Authorization") String authorization,
    @Body Map<String, Object> request
);
```

###  Modèles & Données

#### `Note.java`
Classe POJO représentant une note sauvegardée.

```java
public class Note {
    public long id;
    public String userId;
    public String title;
    public String rawText;
    public String summary;
    public String keywords;
    public long createdAt;
}
```

#### `NotesDbHelper.java`
Hérite de `SQLiteOpenHelper`, crée et met à jour le schéma de la base de données.

**Schéma :**
```sql
CREATE TABLE notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    title TEXT,
    raw_text TEXT,
    summary TEXT,
    keywords TEXT,
    created_at INTEGER
);
```

#### `NotesRepository.java`
Abstraction pour l'accès aux données SQLite. Méthodes :
- `insertNote()` - Insérer une nouvelle note
- `getNotesForUser()` - Récupérer les notes avec recherche optionnelle
- `getNoteById()` - Charger une note par ID
- `deleteNote()` - Supprimer une note
- `cursorToNote()` - Convertir Cursor → Note

###  UI & Listes

#### `NotesListActivity.java`
Affiche la liste des notes de l'utilisateur. Barre de recherche à temps réel, boutons Capture/Logout.

**Intégration :**
```java
String searchQuery = searchEditText.getText().toString();
List<Note> notes = repository.getNotesForUser(userId, searchQuery);
adapter.setNotes(notes);
```

#### `NotesAdapter.java`
Adapter RecyclerView pour afficher les notes dans une liste scrollable. Clique sur une note → `NoteDetailActivity`.

#### `NoteDetailActivity.java`
Affiche les détails complets d'une note sélectionnée : titre, texte brut, résumé, mots-clés, date de création.

---

##  Configuration & Setup

### Prérequis
- Android SDK 24+ (minSdk) / 36 (compileSdk)
- Java 11
- Gradle 8.x
- Firebase Project

### Installation

1. **Cloner le projet**
   ```bash
   git clone <repo-url>
   cd smart_scan_mobile
   ```

2. **Configurer Firebase**
   - Créer un projet Firebase
   - Télécharger `google-services.json`
   - Placer dans `app/` 
   - Activer Email/Password Authentication

3. **Configurer Hugging Face Token**
   - Créer le fichier `.env` à la racine du projet (ou copier depuis le modèle) :
     ```
     HUGGING_FACE_TOKEN=your_token_here
     ```
   - Le token sera automatiquement injecté dans `BuildConfig` lors du build

4. **Build & Run**
   ```bash
   ./gradlew assembleDebug   # Build APK debug
   ./gradlew installDebug    # Installer sur le device
   ./gradlew runDebug        # Lancer l'app
   ```

---

## 📦 Dépendances Clés

### Firebase
- `firebase-auth` - Authentification

### Vision & ML
- `mlkit-text-recognition` - OCR (Google ML Kit)

### Réseau & HTTP
- `retrofit` - Client HTTP
- `retrofit-converter-gson` - Sérialisation JSON
- `okhttp-logging` - Logging HTTP (debugging)

### UI Android
- `appcompat`
- `material` - Composants Material Design
- `constraintlayout`
- `recyclerview`

### Caméra
- `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-extensions` (CameraX)

---

##  Gestion des Secrets

- **Token Hugging Face** : Stocké dans `.env` (exclu de Git via `.gitignore`)
- **Firebase** : Configuré via `google-services.json` (exclu de Git)
- **Gradle Properties** : Les variables d'environnement sont lues au moment du build et injectées via `BuildConfig`

```kotlin
// app/build.gradle.kts
val huggingFaceToken = env["HUGGING_FACE_TOKEN"] ?: "PLACEHOLDER_TOKEN"
buildConfigField("String", "HUGGING_FACE_TOKEN", "\"$huggingFaceToken\"")
```

---

##  Flux Utilisateur Complet

1. **Authentification** → User se connecte avec Firebase (MainActivity)
2. **Capture** → User ouvre CaptureActivity, prend une photo
3. **OCR** → ML Kit extrait le texte de l'image
4. **Résumé** → SummaryService envoie à Hugging Face (bart-large-cnn)
5. **Sauvegarde** → NotesRepository insère en SQLite
6. **Liste & Recherche** → NotesListActivity affiche les notes (avec recherche)
7. **Détail** → NoteDetailActivity affiche la note complète
8. **Suppression** → Repository.deleteNote() si souhaité

---

## 🧪 Tests & Debugging

### Test Hugging Face API (Postman)
Pour tester manuellement le service de résumé sans lancer l'application :

1. **URL** : `POST https://router.huggingface.co/hf-inference/models/facebook/bart-large-cnn`
2. **Headers** :
   - `Authorization`: `Bearer YOUR_HF_TOKEN`
   - `Content-Type`: `application/json`
3. **Body (JSON)** :
```json
{
  "inputs": "Votre texte long à résumer ici...",
  "parameters": {
    "max_length": 120,
    "min_length": 30,
    "do_sample": false
  }
}
```



##  Fichiers Supprimés / Nettoyage

- ✅ `.env` ajouté à `.gitignore`
- ✅ Token supprimé du code source (hardcoded → BuildConfig)
- ✅ Fichiers de build (build/, capture/) ne sont pas committés

---


