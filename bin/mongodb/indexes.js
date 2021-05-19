db.challenge.createIndex(
  { seenAt: 1 },
  { partialFilterExpression: { status: 10, timeControl: { $exists: true }, seenAt: { $exists: true } } }
);

db.simul.createIndex({ hostId: 1 }, { partialFilterExpression: { status: 10 } });
db.simul.createIndex({ hostSeenAt: -1 }, { partialFilterExpression: { status: 10, featurable: true } });
