db.swiss
  .find({
    featurable: true,
    startsAt: { $lt: new Date(Date.now() - 1000 * 3600 * 24) },
    'settings.i': { $lt: 3600 * 24 },
  })
  .forEach(s => {
    print(`FINISH ${s._id} ${s.name} round:${s.round}`);
    db.swiss.update(
      { _id: s._id },
      {
        $set: {
          finishedAt: new Date(),
          'settings.n': s.round,
          canceled: new Date(),
        },
        $unset: {
          nextRoundAt: 1,
          featurable: 1,
        },
      },
    );
  });

db.swiss.find({ nbOngoing: { $ne: 0 } }).forEach(s => {
  const count = db.swiss_pairing.count({ s: s._id, t: true });
  if (count != s.nbOngoing) {
    print(`nbOngoing ${s._id} ${s.name} ${s.nbOngoing} -> ${count}`);
    const set = {
      nbOngoing: NumberInt(count),
    };
    if (!s.finishedAt) set.nextRoundAt = new Date(Date.now() + 1000 * 30);
    db.swiss.update({ _id: s._id }, { $set: set });
  }
});
