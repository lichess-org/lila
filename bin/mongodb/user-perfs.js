db.user4.aggregate([
  { $match: { perfs: { $exists: true, $ne: {} } } },
  { $project: { perfs: 1 } },
  {
    $replaceWith: { $mergeObjects: [{ _id: '$_id' }, '$perfs'] },
  },
  { $out: 'user_perf' },
]);
