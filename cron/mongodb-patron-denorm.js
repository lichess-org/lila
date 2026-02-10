// denormalizes the patron total donation
// to display a full list of donors on /patron

// if !full, only denormalizes the users who have donated in the last period
// ~~we do full once a day around 3am to catch closed accounts~~
// [EDIT] nope! it's too slow to run the full collection, see
// https://monitor.lichess.ovh/d/vzJ2hqYZz/mongotop-all-per-operation?orgId=1&var-host=kaiju&var-op=All&from=1729131327131&to=1729140255526
const full = false; // new Date().getHours() === 3;

const period = 1000 * 60 * 10;

const recentPatrons = db.plan_charge.aggregate([
  { $match: { userId: { $exists: 1 }, date: { $gt: new Date(Date.now() - period * 1.1) } } },
  { $group: { _id: null, users: { $addToSet: '$userId' } } },
]);

const userSelect = full
  ? []
  : [
      {
        $match: {
          userId: {
            $in: recentPatrons.hasNext() ? recentPatrons.next().users : [],
          },
        },
      },
    ];

print(full ? 'Full denormalization' : `Denormalizing ${userSelect[0].$match.userId.$in.length} users`);

db.plan_charge
  .aggregate([
    ...userSelect,
    { $group: { _id: '$userId', a: { $sum: '$usd' } } },
    {
      $lookup: {
        from: 'user4',
        as: 'user',
        let: { id: '$_id' },
        pipeline: [
          {
            $match: {
              $expr: {
                $and: [{ $eq: ['$_id', '$$id'] }],
              },
            },
          },
          { $project: { _id: 0, enabled: 1 } },
        ],
      },
    },
    { $unwind: '$user' },
    {
      $lookup: {
        from: 'plan_patron',
        as: 'patron',
        localField: '_id',
        foreignField: '_id',
      },
    },
    { $unwind: '$patron' },
    {
      $addFields: {
        score: { $cond: ['$user.enabled', { $toInt: '$a' }, null] },
      },
    },
    {
      $match: {
        $expr: { $ne: ['$score', '$patron.score'] },
      },
    },
    { $project: { _id: 1, score: 1 } },
  ])
  .forEach(x => {
    print(x._id, x.score);
    db.plan_patron.updateOne({ _id: x._id }, { $set: { score: x.score } });
  });
