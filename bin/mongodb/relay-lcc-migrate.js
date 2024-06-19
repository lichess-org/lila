const regex = /.*view\.livechesscloud\.com\/?#?([0-9a-f\-]+)/;
let done = 0;
let failed = 0;
db.relay.find({ 'sync.upstream.url': /view\.livechesscloud\.com/ }).forEach(relay => {
  const url = relay.sync.upstream.url;
  try {
    const id = url.match(regex)[1];
    const round = parseInt(url.split(' ')[1]) || 1;
    if (!id) throw new Error('No id in ' + url);
    db.relay.updateOne({ _id: relay._id }, { $set: { 'sync.upstream': { lcc: id, round } } });
    done++;
  } catch (e) {
    failed++;
  }
});
console.log(done + ' done');
console.log(failed + ' failed');
