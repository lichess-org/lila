/* O(1) assuming constant number of recently updated posts
 * At the time of writing, with 48k ublog_post in the DB,
 * it takes about 4 seconds to run and uses up to 300MB of memory.
 *
 * Should run periodically, e.g. every 1 hour.
 */
const nbSimilar = 6;
const since = new Date(Date.now() - 1000 * 60 * 60 * 24 * 15);
const updatable = db.ublog_post.find({ live: true, 'updated.at': { $gt: since } }, { likers: 1 }).toArray();
console.log(`${updatable.length} posts were updated since ${since}`);

const updatableLikers = new Set();
updatable.forEach(p => p.likers.forEach(l => updatableLikers.add(l)));
console.log(`They have ${updatableLikers.size} likers.`);

console.log(`Computing liker->ids...`);
const likerToIds = new Map();
db.ublog_post.find({ live: true, likers: { $in: Array.from(updatableLikers) } }, { likers: 1 }).forEach(p => {
  updatableLikers.intersection(new Set(p.likers)).forEach(l => {
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
  const top3 = Array.from(similar)
    .sort((a, b) => b[1] - a[1])
    .slice(0, nbSimilar);
  db.ublog_post.updateOne({ _id: p._id }, { $set: { similar: top3.map(([id, _]) => id) } });
});
