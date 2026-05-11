Mark existing users as over-13

Overview

This folder contains a small Node.js script to mark all existing user documents in Firestore with `isOver13Confirmed: true`.

Security

- DO NOT commit your `serviceAccountKey.json` into source control.
- Run this script from a secure machine with the service account key.
- Make a backup/export of your `users` collection before running bulk updates.

Setup

1. Place your Firebase service account JSON file (download from Firebase Console -> Project Settings -> Service Accounts) somewhere safe, e.g. `~/secrets/serviceAccountKey.json`.
2. From the repo root, install dependency:

```bash
npm init -y
npm install firebase-admin
```

Run

```bash
node scripts/mark_over13.js /full/path/to/serviceAccountKey.json
```

Notes

- The script uses Firestore batch writes (500 operations per batch) and uses `set(..., { merge: true })` so it won't overwrite other fields.
- If you prefer to mark only specific users, edit the script to add filters (for example, query by `createdAt` or `email`).

Kotlin snippet (alternative, single-user update from app)

If you want to mark a single user from the Android app (admin-only action), use this snippet in a secure admin-only screen:

```kotlin
val db = FirebaseFirestore.getInstance()
val uid = "UID_DEL_USUARIO"
db.collection("users").document(uid)
  .set(mapOf("isOver13Confirmed" to true), SetOptions.merge())
  .addOnSuccessListener { Log.d("Update","ok") }
  .addOnFailureListener { e -> Log.e("Update", e.message ?: "") }
```

If you want, I can add a small admin endpoint or an admin-only in-app function to trigger this safely.