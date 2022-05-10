db.user4
  .find({ title: 'BOT', 'count.winH': { $gt: 100 } })
  .sort({ createdAt: -1 })
  .limit(1000)
  .forEach(user => {
    agg = db.game5
      .aggregate([
        { $match: { us: user._id } },
        { $project: { wid: 1, t: 1, ra: 1 } },
        {
          $addFields: {
            sus: { $and: [{ $eq: ['$wid', user._id] }, { $lt: ['$t', 10] }, { $ne: ['$ra', true] }] },
          },
        },
        { $sort: { ca: -1 } },
        { $limit: 1000 },
        {
          $group: {
            _id: '$sus',
            nb: { $sum: 1 },
          },
        },
      ])
      .toArray();
    oks = agg.filter(o => !o._id)[0];
    oks = oks ? oks.nb : 0;
    kos = agg.filter(o => o._id)[0];
    kos = kos ? kos.nb : 0;
    ratio = kos / (oks + kos);
    if (ratio > 0.5) {
      print(`https://lichess.org/@/${user.username} ${user.enabled} ${kos} / ${oks + kos}`);
    }
  });
