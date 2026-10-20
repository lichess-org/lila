// Phase 7.2: normalize stored emails to the same form the app writes
// (EmailAddress.normalize): lowercase, strip +tag, and for gmail-like
// domains also strip dots. Idempotent, non-destructive besides normalization.
//
//   mongosh lila bin/mongodb/migrations/2026-10-20-normalize-emails.js
//
// The original address, when it differed, remains in `verbatimEmail`.

const gmailLike = new Set(['gmail.com', 'googlemail.com', 'protonmail.com', 'protonmail.ch', 'pm.me']);

function normalize(email) {
  const lower = String(email).toLowerCase();
  const at = lower.lastIndexOf('@');
  if (at < 1) return lower;
  const name = lower.slice(0, at);
  const domain = lower.slice(at + 1);
  const withoutTag = name.split('+')[0];
  const normalizedName = gmailLike.has(domain) ? withoutTag.replace(/\./g, '') : withoutTag;
  return normalizedName ? `${normalizedName}@${domain}` : lower;
}

let changed = 0;

db.user4.find({ email: { $exists: true } }).forEach(u => {
  const normalized = normalize(u.email);
  if (normalized !== u.email) {
    const set = { email: normalized };
    if (!u.verbatimEmail) set.verbatimEmail = u.email;
    db.user4.updateOne({ _id: u._id }, { $set: set });
    changed++;
  }
});

db.user4.find({ prevEmail: { $exists: true } }).forEach(u => {
  const normalized = normalize(u.prevEmail);
  if (normalized !== u.prevEmail) {
    db.user4.updateOne({ _id: u._id }, { $set: { prevEmail: normalized } });
    changed++;
  }
});

print(`normalized ${changed} email field(s)`);

const duplicates = db.user4
  .aggregate([
    { $match: { email: { $exists: true } } },
    { $group: { _id: '$email', n: { $sum: 1 }, ids: { $push: '$_id' } } },
    { $match: { n: { $gt: 1 } } },
    { $sort: { n: -1 } },
  ])
  .toArray();

if (duplicates.length === 0) {
  print('no duplicate emails');
} else {
  print(`duplicate emails: ${duplicates.length} group(s) — review, do not delete automatically`);
  duplicates.slice(0, 50).forEach(d => printjson(d));
}
