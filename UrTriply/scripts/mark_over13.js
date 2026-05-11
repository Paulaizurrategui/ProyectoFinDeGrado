// scripts/mark_over13.js
// Usage: node mark_over13.js ./path/to/serviceAccountKey.json
// Requires: npm install firebase-admin

const admin = require('firebase-admin');
const fs = require('fs');

const serviceAccountPath = process.argv[2] || './serviceAccountKey.json';
if (!fs.existsSync(serviceAccountPath)) {
  console.error('serviceAccountKey.json not found. Provide its path as the first argument.');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(require(serviceAccountPath))
});

const db = admin.firestore();

async function markAllOver13() {
  const usersSnap = await db.collection('users').get();
  console.log(`Found ${usersSnap.size} user documents`);

  const docs = usersSnap.docs;
  let batch = db.batch();
  const BATCH_LIMIT = 500; // Firestore batch write limit
  let batchCount = 0;
  let totalUpdated = 0;

  for (let i = 0; i < docs.length; i++) {
    const doc = docs[i];
    // Use set with merge to avoid overwriting other fields
    batch.set(doc.ref, { isOver13Confirmed: true }, { merge: true });
    batchCount++;

    if (batchCount >= BATCH_LIMIT) {
      await batch.commit();
      totalUpdated += batchCount;
      console.log(`Committed batch of ${batchCount} (total ${totalUpdated})`);
      batch = db.batch();
      batchCount = 0;
    }
  }

  if (batchCount > 0) {
    await batch.commit();
    totalUpdated += batchCount;
    console.log(`Committed final batch of ${batchCount} (total ${totalUpdated})`);
  }

  console.log(`Done. Updated ${totalUpdated} user documents.`);
}

markAllOver13().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
