db.modlog.update(
  { human: { $ne: true }, mod: { $ne: 'lichess' } },
  { $set: { human: true } },
  { multi: true },
);
db.modlog.createIndex({ mod: 1, date: -1 }, { partialFilterExpression: { human: true } });

db.report2.find({ processedBy: { $exists: 1 } }).forEach(r =>
  db.report2.update(
    { _id: r._id },
    {
      $unset: { processedBy: 1 },
      $set: {
        done: {
          by: r.processedBy,
          at: r.atoms[0].at,
        },
      },
    },
  ),
);
db.report2.createIndex({ 'done.at': -1 }, { partialFilterExpression: { open: false } });
