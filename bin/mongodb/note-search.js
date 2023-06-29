db.note
  .find(
    {
      s: { $ne: true },
      mod: true,
      dox: { $ne: true },
      from: { $nin: ['watcherbot', 'lichess'] },
      text: { $not: /^Appeal reply:/ },
    },
    { _id: 1 }
  )
  .forEach(n => db.note.updateOne(n, { $set: { s: true } }));

db.note
  .find(
    {
      s: true,
      text: /^Appeal reply:/,
    },
    { _id: 1 }
  )
  .forEach(n => db.note.updateOne(n, { $unset: { s: true } }));
