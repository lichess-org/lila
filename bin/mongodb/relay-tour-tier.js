db.relay_tour.update(
  { official: true },
  { $set: { tier: NumberInt(3) }, $unset: { official: true } },
  { multi: 1 },
);
db.relay_tour.dropIndex('active_1_official_1_syncedAt_-1');
db.relay_tour.createIndex(
  { active: 1, tier: 1 },
  { partialFilterExpression: { active: true, tier: { $exists: true } } },
);
db.relay_tour.createIndex({ syncedAt: -1 }, { partialFilterExpression: { tier: { $exists: true } } });
