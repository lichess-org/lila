// Read-only audit for the OIDC/SSO migration.
// Run against a restored copy, never against production:
//   mongosh lichess /tmp/audit-oidc-migration.js
//
// Prints aggregates and masked samples only (no emails, no full usernames).

const now = new Date();
const DAY = 24 * 60 * 60 * 1000;

const ID_REGEX =
  /^(?:[1-9][0-9]{0,30}|lichess|ghost|broadcaster|irwin|kaladin|openingexplorer|lichess4545|challengermode|watcherbot|![A-Za-z0-9_-]{1,32})$/;
const DISPLAY_REGEX = /^[\p{L}\p{N}_\- ]{2,30}$/u;
const SYSTEM_IDS = new Set([
  'lichess',
  'ghost',
  'broadcaster',
  'irwin',
  'kaladin',
  'openingexplorer',
  'lichess4545',
  'challengermode',
  'watcherbot',
]);
const GMAIL_LIKE = new Set(['gmail.com', 'googlemail.com', 'protonmail.com', 'protonmail.ch', 'pm.me']);

function mask(value) {
  if (value == null) return null;
  const s = String(value);
  return s.length <= 2 ? '***' : s.slice(0, 2) + '***';
}

function normalizeEmail(email) {
  const lower = String(email).toLowerCase();
  const at = lower.lastIndexOf('@');
  if (at < 1) return lower;
  const name = lower.slice(0, at);
  const domain = lower.slice(at + 1);
  const withoutTag = name.split('+')[0];
  const normalizedName = GMAIL_LIKE.has(domain) ? withoutTag.replace(/\./g, '') : withoutTag;
  return normalizedName ? `${normalizedName}@${domain}` : lower;
}

function inc(map, key) {
  map[key] = (map[key] || 0) + 1;
}

const users = db.user4
  .find(
    {},
    {
      _id: 1,
      username: 1,
      email: 1,
      prevEmail: 1,
      verbatimEmail: 1,
      enabled: 1,
      mustConfirmEmail: 1,
      delete: 1,
      foreverClosed: 1,
      len: 1,
      createdAt: 1,
    },
  )
  .toArray();

const idSet = new Set(users.map(u => String(u._id).toLowerCase()));
const usernameToIds = new Map();

let enabled = 0;
let disabled = 0;
let deleted = 0;
let foreverClosed = 0;
let mustConfirm = 0;
let missingEmail = 0;
let emailNormalizationChanges = 0;
let missingLen = 0;
let wrongLen = 0;
let missingCreatedAt = 0;
let legacyId = 0;
let systemId = 0;
let ghostId = 0;
let displayOk = 0;
const displayProblems = {};
const usernameCollidesWithOtherId = [];
const usernameEqualsOwnId = [];
const numericUsername = [];
let cyrillic = 0;
let withSpace = 0;
let badEdgeSpace = 0;

const emailCounts = new Map();
const emailSamples = [];
const usernameCases = new Map();

for (const u of users) {
  const id = String(u._id);
  const idLower = id.toLowerCase();
  const username = u.username == null ? '' : String(u.username);

  if (u.enabled === true) enabled++;
  else disabled++;
  if (u.delete && u.delete.done) deleted++;
  if (u.foreverClosed) foreverClosed++;
  if (u.mustConfirmEmail) mustConfirm++;
  if (u.email == null) missingEmail++;
  if (u.len == null) missingLen++;
  else if (u.len !== username.length) wrongLen++;
  if (u.createdAt == null) missingCreatedAt++;

  if (/^![A-Za-z0-9_-]{1,32}$/.test(id)) ghostId++;
  else if (SYSTEM_IDS.has(idLower)) systemId++;
  else if (!ID_REGEX.test(idLower)) legacyId++;

  if (DISPLAY_REGEX.test(username)) displayOk++;
  else {
    inc(displayProblems, 'notDisplayRegex');
    if (username.length < 2) inc(displayProblems, 'tooShort');
    if (username.length > 30) inc(displayProblems, 'tooLong');
    if (username !== username.trim()) inc(displayProblems, 'edgeWhitespace');
    if (/^\s+$/.test(username)) inc(displayProblems, 'blank');
  }
  if (/[А-Яа-яЁё]/.test(username)) cyrillic++;
  if (username.includes(' ')) withSpace++;
  if (username !== username.trim()) badEdgeSpace++;

  const lowerUsername = username.toLowerCase();
  if (/^[1-9][0-9]{0,30}$/.test(lowerUsername)) {
    numericUsername.push(id);
    if (idSet.has(lowerUsername) && lowerUsername !== idLower)
      usernameCollidesWithOtherId.push({ id: mask(id), username: mask(username) });
  }
  if (lowerUsername === idLower) usernameEqualsOwnId.push(mask(id));

  const ids = usernameToIds.get(lowerUsername) || [];
  ids.push(id);
  usernameToIds.set(lowerUsername, ids);

  if (u.email != null) {
    const normalized = normalizeEmail(u.email);
    const entry = emailCounts.get(normalized) || { count: 0, ids: [] };
    entry.count++;
    if (entry.ids.length < 3) entry.ids.push(mask(id));
    emailCounts.set(normalized, entry);
    if (String(u.email) !== normalized) {
      emailNormalizationChanges++;
      if (emailSamples.length < 5)
        emailSamples.push({ id: mask(id), raw: mask(u.email), normalized: mask(normalized) });
    }
  }
}

let duplicateEmailGroups = 0;
let duplicateEmailUsers = 0;
const duplicateEmailSamples = [];
for (const [email, entry] of emailCounts) {
  if (entry.count > 1) {
    duplicateEmailGroups++;
    duplicateEmailUsers += entry.count;
    if (duplicateEmailSamples.length < 5)
      duplicateEmailSamples.push({ email: mask(email), users: entry.count, ids: entry.ids });
  }
}

let duplicateUsernameGroups = 0;
let duplicateUsernameUsers = 0;
const duplicateUsernameSamples = [];
for (const [name, ids] of usernameToIds) {
  if (ids.length > 1) {
    duplicateUsernameGroups++;
    duplicateUsernameUsers += ids.length;
    if (duplicateUsernameSamples.length < 5)
      duplicateUsernameSamples.push({
        username: mask(name),
        users: ids.length,
        ids: ids.slice(0, 3).map(mask),
      });
  }
}

const security = db.security;
const securityTotal = security.countDocuments();
const securityMissingExpiresAt = security.countDocuments({ expiresAt: { $exists: false } });
const securityMissingSid = security.countDocuments({ sid: { $exists: false } });
const securityMissingIdToken = security.countDocuments({ idToken: { $exists: false } });
const securityUpFalse = security.countDocuments({ up: false });
const securityExpired = security.countDocuments({ expiresAt: { $lt: now } });
const securityOlder30d = security.countDocuments({ date: { $lt: new Date(now - 30 * DAY) } });

const perUser = security
  .aggregate([
    { $match: { user: { $exists: true } } },
    { $group: { _id: '$user', n: { $sum: 1 } } },
    { $group: { _id: null, users: { $sum: 1 }, max: { $max: '$n' }, avg: { $avg: '$n' } } },
  ])
  .toArray()[0] || { users: 0, max: 0, avg: 0 };

printjson({
  users: {
    total: users.length,
    enabled,
    disabled,
    deletedTombstones: deleted,
    foreverClosed,
    mustConfirmEmail: mustConfirm,
    missingEmail,
    missingLen,
    wrongLen,
    missingCreatedAt,
    ids: {
      canonicalNumeric: users.length - legacyId - systemId - ghostId,
      legacy: legacyId,
      system: systemId,
      ghost: ghostId,
    },
    usernames: {
      displayOk,
      problems: displayProblems,
      cyrillic,
      withSpace,
      badEdgeSpace,
      numeric: numericUsername.length,
      equalsOwnId: usernameEqualsOwnId.length,
      collidesWithAnotherId: usernameCollidesWithOtherId.length,
      collideSamples: usernameCollidesWithOtherId.slice(0, 5),
      duplicateGroups: duplicateUsernameGroups,
      duplicateUsers: duplicateUsernameUsers,
      duplicateSamples: duplicateUsernameSamples,
    },
    emails: {
      normalizationChanges: emailNormalizationChanges,
      duplicateGroups: duplicateEmailGroups,
      duplicateUsers: duplicateEmailUsers,
      normalizedChangeSamples: emailSamples,
      duplicateSamples: duplicateEmailSamples,
    },
  },
  security: {
    total: securityTotal,
    missingExpiresAt: securityMissingExpiresAt,
    missingSid: securityMissingSid,
    missingIdToken: securityMissingIdToken,
    upFalse: securityUpFalse,
    expired: securityExpired,
    olderThan30d: securityOlder30d,
    distinctUsers: perUser.users,
    maxSessionsPerUser: perUser.max,
    avgSessionsPerUser: perUser.avg,
  },
  oauth: {
    accessTokens: db.oauth_access_token.countDocuments(),
    oauth2AccessTokens: db.oauth2_access_token.countDocuments(),
  },
  appeal: { sessions: db.appeal.countDocuments() },
});
