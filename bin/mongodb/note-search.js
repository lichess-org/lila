db.note.dropIndex('search');

db.note
  .find(
    {
      s: { $ne: true },
      mod: true,
      from: { $nin: ['watcherbot', 'lichess'] },
      text: { $not: /^Appeal reply:/ },
    },
    { _id: 1 },
  )
  .forEach(n => db.note.updateOne(n, { $set: { s: true } }));

db.note.createIndex(
  { text: 'text', from: 'text', to: 'text', dox: 1, date: -1 },
  { name: 'search', partialFilterExpression: { s: true } },
);
