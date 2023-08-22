db.study.createIndex(
  { topics: 1, rank: -1 },
  { partialFilterExpression: { topics: { $exists: 1 } }, background: 1 },
);
db.study.createIndex(
  { topics: 1, createdAt: -1 },
  { partialFilterExpression: { topics: { $exists: 1 } }, background: 1 },
);
db.study.createIndex(
  { topics: 1, updatedAt: -1 },
  { partialFilterExpression: { topics: { $exists: 1 } }, background: 1 },
);
db.study.createIndex(
  { topics: 1, likes: -1 },
  { partialFilterExpression: { topics: { $exists: 1 } }, background: 1 },
);
db.study.createIndex(
  { uids: 1, rank: -1 },
  { partialFilterExpression: { topics: { $exists: 1 } }, background: 1 },
);
