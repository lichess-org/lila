db.user4.aggregate([
  { $match: { perfs: { $exists: true, $ne: {} }, seenAt: { $gt: new Date(Date.now() - 1000 * 3600) } } },
  { $project: { perfs: 1 } },
  {
    $replaceWith: { $mergeObjects: [{ _id: '$_id' }, '$perfs'] },
  },
  // { $out: 'user_perf' },
  { $merge: { into: 'user_perf', on: '_id', whenMatched: 'replace', whenNotMatched: 'insert' } },
]);
