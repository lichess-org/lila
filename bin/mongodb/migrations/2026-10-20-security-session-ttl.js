// Phase 4.5: existing sessions predate `expiresAt`, so a TTL index alone
// would never remove them. Backfill recent sessions and drop sessions older
// than the 30-day policy.
//
//   mongosh lila bin/mongodb/migrations/2026-10-20-security-session-ttl.js
//
// Run during a maintenance window: this logs out sessions older than 30 days.

const MAX_AGE_MS = 30 * 24 * 60 * 60 * 1000;
const now = new Date();
const cutoff = new Date(now.getTime() - MAX_AGE_MS);

const total = db.security.countDocuments();
const missing = db.security.countDocuments({ expiresAt: { $exists: false } });
const missingDate = db.security.countDocuments({ expiresAt: { $exists: false }, date: { $exists: false } });
const old = db.security.countDocuments({ expiresAt: { $exists: false }, date: { $lt: cutoff } });
const recent = db.security.countDocuments({ expiresAt: { $exists: false }, date: { $gte: cutoff } });

const backfilled = db.security.updateMany({ expiresAt: { $exists: false }, date: { $gte: cutoff } }, [
  { $set: { expiresAt: { $add: ['$date', MAX_AGE_MS] } } },
]);

const removedOld = db.security.deleteMany({ expiresAt: { $exists: false }, date: { $lt: cutoff } });
const removedNoDate = db.security.deleteMany({ expiresAt: { $exists: false }, date: { $exists: false } });

printjson({
  total,
  missingExpiresAt: missing,
  oldSessionsToDrop: old,
  recentSessionsToBackfill: recent,
  missingDateToDrop: missingDate,
  backfilled: backfilled.modifiedCount,
  removedOld: removedOld.deletedCount,
  removedNoDate: removedNoDate.deletedCount,
  remaining: db.security.countDocuments(),
  remainingWithoutExpiresAt: db.security.countDocuments({ expiresAt: { $exists: false } }),
});
