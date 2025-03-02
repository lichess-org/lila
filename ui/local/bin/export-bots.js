const docs = [];

db.local_bot
  .aggregate([
    { $sort: { version: -1 } },
    { $group: { _id: '$uid', doc: { $first: '$$ROOT' } } },
    { $replaceRoot: { newRoot: '$doc' } },
  ])
  .forEach(doc => docs.push(doc));

print(JSON.stringify(docs, null, 2));
