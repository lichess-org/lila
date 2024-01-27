print('Backfilling user playTime, for https://github.com/lichess-org/lila/issues/6995');

let currentRecord = 1;

db.user4
  .find(
    {
      // recently active users without a `time` field
      time: { $exists: false },
      'count.game': { $gte: 1 },
      seenAt: { $gte: ISODate('2023-10-01T00:00:00Z') },
    },
    { _id: 1 },
  )
  .forEach(user => {
    let result = db.game5
      .aggregate([
        {
          $match: {
            us: user._id,
            c: { $exists: true },
          },
        },
        {
          $project: {
            duration: {
              $subtract: ['$ua', '$ca'],
            },
          },
        },
        {
          $match: {
            // ignore games over 4 hours (either correspondence or bad data)
            duration: { $lte: 4 * 60 * 60 * 1000 },
          },
        },
        {
          $group: {
            _id: null,
            totalDuration: {
              $sum: '$duration',
            },
          },
        },
      ])
      .toArray();

    if (result.length === 0) {
      print(currentRecord++, user._id, 'skipping');
      return;
    }

    let duration = NumberInt(Math.floor(result[0].totalDuration / 1000));

    db.user4.updateOne(
      { _id: user._id },
      {
        $set: {
          time: {
            total: duration,
            tv: 0,
          },
        },
      },
    );

    print(currentRecord++, user._id, duration);
  });

print('Done');
