// Phase 3.6/3.2: `len` is used for username ordering/search. Existing rows
// may have stale values (prod audit found ~9% mismatches).
//
//   mongosh lila bin/mongodb/migrations/2026-10-20-recompute-username-len.js
//
// Idempotent.

let changed = 0;
let missing = 0;

db.user4.find({}, { username: 1, len: 1 }).forEach(u => {
  const len = u.username == null ? 0 : String(u.username).length;
  if (u.len === len) return;
  if (u.len == null) missing++;
  db.user4.updateOne({ _id: u._id }, { $set: { len } });
  changed++;
});

printjson({ scanned: db.user4.countDocuments(), changed, missingLen: missing });
