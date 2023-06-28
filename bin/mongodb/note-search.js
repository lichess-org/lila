db.note
  .find(
    {
      s: { $ne: true },
      mod: true,
      dox: { $ne: true },
      from: { $nin: ['watcherbot', 'lichess'] },
    },
    { _id: 1 }
  )
  .forEach(n => db.note.updateOne(n, { $set: { s: true } }));
