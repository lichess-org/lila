db.tournament2.update({ createdBy: 'lichess' }, { $unset: { createdBy: 1 } }, { multi: 1 });
db.tournament2.dropIndex('createdBy_1_startsAt_-1');
db.tournament2.createIndex(
  { createdBy: 1, startsAt: -1, status: 1 },
  { partialFilterExpression: { createdBy: { $exists: true } } },
);
