db.challenge.createIndex(
  { seenAt: 1 },
  { partialFilterExpression: { status: 10, timeControl: { $exists: true }, seenAt: { $exists: true } } }
);

db.simul.createIndex({ hostId: 1 }, { partialFilterExpression: { status: 10 } });
db.simul.createIndex({ hostSeenAt: -1 }, { partialFilterExpression: { status: 10, featurable: true } });

db.oauth2_access_token.createIndex({ userId: 1 });
db.oauth2_access_token.createIndex({ expires: 1 }, { expireAfterSeconds: 0 });
db.oauth2_authorization.createIndex({ expires: 1 }, { expireAfterSeconds: 0 });

db.cache.createIndex({ e: 1 }, { expireAfterSeconds: 0 });
