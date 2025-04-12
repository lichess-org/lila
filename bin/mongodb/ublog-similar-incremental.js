/* O(1) assuming constant number of recently updated posts
 * At the time of writing, with 48k ublog_post in the DB,
 * it takes about 4 seconds to run and uses up to 300MB of memory.
 *
 * Should run periodically, e.g. every 1 hour.
 */
const nbSimilar = 6;
const minTier = 2;
const since = new Date(Date.now() - 1000 * 60 * 60 * 24 * 14);

const updatable = db.ublog_post.aggregate([
  { $match: { live: true, 'updated.at': { $gt: since } } },
  {
    $lookup: {
      let: { id: '$blog' }, pipeline: [
        { $match: { $expr: { $and: [{ $eq: ['$_id', '$$id'] }, { $gte: ['$tier', minTier] }] } } },
        { $project: { _id: 1 } }
      ], from: 'ublog_blog', as: 'blog'
    }
  }, { $unwind: '$blog' }, { $project: { likers: 1 } }]).toArray();

console.log(`${updatable.length} listed posts were updated since ${since}`);

const updatableLikers = new Set();
updatable.forEach(p => p.likers.forEach(l => updatableLikers.add(l)));
console.log(`They have ${updatableLikers.size} likers.`);

console.log(`Computing liker->ids...`);
const likerToIds = new Map();
db.ublog_post.find({ live: true, likers: { $in: Array.from(updatableLikers) } }, { likers: 1 }).forEach(p => {
  // updatableLikers.intersection(new Set(p.likers)) // TypeError: updatableLikers.intersection is not a function
  p.likers.filter(l => updatableLikers.has(l)).forEach(l => {
    if (!likerToIds.has(l)) likerToIds.set(l, []);
    likerToIds.get(l).push(p._id);
  });
});
console.log(`Updating ${updatable.length} posts...`);

updatable.forEach(p => {
  const similar = new Map();
  p.likers.forEach(liker => {
    (likerToIds.get(liker) || []).forEach(id => {
      if (id != p._id) similar.set(id, (similar.get(id) || 0) + 1);
    });
  });
  const top = Array.from(similar)
    .sort((a, b) => b[1] - a[1])
    .slice(0, nbSimilar);
  db.ublog_post.updateOne({ _id: p._id }, { $set: { similar: top.map(([id, _]) => id) } });
});
