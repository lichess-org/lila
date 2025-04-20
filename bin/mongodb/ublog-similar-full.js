/* O(n) for ublog_post documents
 * for 48k documents in the DB, it takes about 2 minutes to run
 * and uses up to 600MB of memory.
 *
 * Should run only once, then be replaced with
 * ublog-graph-incremental.js
 */
const nbSimilar = 6;
const minTier = 2;

print('Deleting all post.similar');

db.ublog_post.updateMany({}, { $unset: { similar: 1 } });

print('Full recompute');

const all = db.ublog_post
  .aggregate([
    { $match: { live: true, 'likers.1': { $exists: true } } },
    {
      $lookup: {
        let: { id: '$blog' },
        pipeline: [
          { $match: { $expr: { $and: [{ $eq: ['$_id', '$$id'] }, { $gte: ['$tier', minTier] }] } } },
          { $project: { _id: 1 } },
        ],
        from: 'ublog_blog',
        as: 'blog',
      },
    },
    { $unwind: '$blog' },
    { $project: { likers: 1 } },
  ])
  .toArray();
print(`${all.length} listed posts to go.`);

print(`Computing likers...`);
const likers = new Map();
all.forEach(p => {
  p.likers.forEach(l => {
    if (!likers.has(l)) likers.set(l, []);
    likers.get(l).push(p._id);
  });
});
print(likers.size + ` likers found.`);

print(`Updating posts...`);
all.forEach(p => {
  const similar = new Map();
  p.likers.forEach(liker => {
    (likers.get(liker) || []).forEach(id => {
      if (id != p._id) similar.set(id, (similar.get(id) || 0) + 1);
    });
  });
  const sorted = Array.from(similar)
    .sort((a, b) => b[1] - a[1])
    .slice(0, nbSimilar);
  db.ublog_post.updateOne(
    { _id: p._id },
    { $set: { similar: sorted.map(([id, count]) => ({ id, count })) } },
  );
});
