// Phase 7.1: email is contact data, not an identity key.
// Existing unique index rejects duplicate emails from different SSO subjects.
// Apply once during the deployment window, before enabling multi-subject emails.
//
//   mongosh lila bin/mongodb/migrations/2026-10-20-drop-unique-email-index.js
//
// If the index name differs, list it with db.user4.getIndexes() and drop by name.

const indexes = db.user4.getIndexes();
const emailIndex = indexes.find(i => i.key && i.key.email === 1 && i.unique === true);
if (emailIndex) {
  print(`dropping unique email index: ${emailIndex.name}`);
  db.user4.dropIndex(emailIndex.name);
} else {
  print('no unique email index found');
}
db.user4.createIndex({ email: 1 }, { partialFilterExpression: { email: { $exists: 1 } } });
