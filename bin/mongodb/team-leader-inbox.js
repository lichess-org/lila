// https://www.mongodb.com/docs/manual/reference/operator/aggregation/merge/#std-label-merge-behavior-same-collection
db.msg_thread.aggregate([
  // https://en.wikipedia.org/wiki/Halloween_Problem
  { $match: { maskWith: { $exists: false } } },
  { $addFields: { 'maskWith.date': '$lastMsg.date' } },
  { $merge: 'msg_thread' },
]);
