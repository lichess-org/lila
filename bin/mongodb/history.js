const historyToMigrate = db.user_history.find();
const max = historyToMigrate.count();
const batchSize = 1000;
const collection = db.history;
const games = db.game4;
let dat = new Date().getTime() / 1000,
  it = 0;

print('Migrating ' + max + ' user histories');

collection.drop();

let game;
historyToMigrate.forEach(function (h) {
  const h2 = { _id: h._id, entries: [] };
  for (ts in h.entries) {
    const e = h.entries[ts];
    const e2 = [parseInt(ts), parseInt(e.e)];
    if (e.g) {
      game = games.findOne({ _id: e.g }, { 'p.elo': true, 'p.uid': true });
      if (game) {
        if (game.p[0].uid != h._id) e2.push(game.p[0].elo);
        else e2.push(game.p[1].elo);
      }
    }
    h2.entries.push(e2);
  }
  collection.update({ _id: h2._id }, { $set: h2 }, { upsert: true });
  ++it;
  if (it % batchSize == 0) {
    const percent = Math.round((it / max) * 100);
    const dat2 = new Date().getTime() / 1000;
    const perSec = Math.round(batchSize / (dat2 - dat));
    dat = dat2;
    print(it / 1000 + 'k ' + percent + '% ' + perSec + '/s');
  }
});
