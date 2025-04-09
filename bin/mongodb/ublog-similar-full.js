/* O(n) for ublog_post documents
 * for 48k documents in the DB, it takes about 2 minutes to run
 * and uses up to 600MB of memory.
 *
 * Should run only once, then be replaced with
 * ublog-graph-incremental.js
 */
const nbSimilar = 6;
console.log('Full recompute');
const all = db.ublog_post.find({ live: true, 'likers.1': { $exists: true } }, { likers: 1 }).toArray();
console.log(`${all.length} posts to go.`);

console.log(`Computing likers...`);
const likers = new Map();
all.forEach(p => {
  p.likers.forEach(l => {
    if (!likers.has(l)) likers.set(l, []);
    likers.get(l).push(p._id);
  });
});
console.log(likers.size + ` likers found.`);

console.log(`Updating posts...`);
all.forEach(p => {
  const similar = new Map();
  p.likers.forEach(liker => {
    (likers.get(liker) || []).forEach(id => {
      if (id != p._id) similar.set(id, (similar.get(id) || 0) + 1);
    });
  });
  const top3 = Array.from(similar)
    .sort((a, b) => b[1] - a[1])
    .slice(0, nbSimilar);
  db.ublog_post.updateOne({ _id: p._id }, { $set: { similar: top3.map(([id, _]) => id) } });
});
