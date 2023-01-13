db.insight.aggregate([
  { $match: { $or: [{ 'm.s': { $type: 'double' } }, { 'm.w': { $type: 'double' } }] } },
  {
    $set: {
      m: {
        $map: {
          input: '$m',
          as: 'n',
          in: {
            $mergeObjects: [
              '$$n',
              { $cond: [{ $gt: ['$$n.s', -1] }, { s: { $toInt: { $multiply: ['$$n.s', 1000] } } }, {}] },
              { $cond: [{ $gt: ['$$n.w', -1] }, { w: { $toInt: { $multiply: ['$$n.w', 1000] } } }, {}] },
            ],
          },
        },
      },
    },
  },
  { $merge: 'insight' },
]);
