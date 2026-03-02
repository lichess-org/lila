const gamesToMigrate = db.game4.find({}, { uids: true, 'p.uid': true });
const max = gamesToMigrate.count();
const batchSize = 50000;

print('Migrating ' + max + ' games');

let i,
  j = 0,
  t,
  timeStrings,
  times,
  it = 0;
let dat = new Date().getTime() / 1000;

gamesToMigrate.forEach(function (game) {
  let prev = '',
    uids = [];
  game.p.forEach(function (p) {
    if (p.uid && p.uid != prev) {
      uids.push(p.uid);
      prev = p.uid;
    }
  });

  const gameSris = game.uids || [];
  if (gameSris.length != uids.length) {
    ++j;
    db.game4.update({ _id: game['_id'] }, { $set: { uids: uids } });
  }

  ++it;
  if (it % batchSize == 0) {
    const percent = Math.round((it / max) * 100);
    const dat2 = new Date().getTime() / 1000;
    const perSec = Math.round(batchSize / (dat2 - dat));
    dat = dat2;
    print(it / 1000 + 'k ' + percent + '% ' + perSec + '/s - ' + j + ' updated');
    j = 0;
  }
});
