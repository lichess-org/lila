var gamesToMigrate = db.game4.find({}, { uids: true, 'p.uid': true });
var max = gamesToMigrate.count();
var batchSize = 50000;

print('Migrating ' + max + ' games');

var i,
  j = 0,
  t,
  timeStrings,
  times,
  it = 0;
var dat = new Date().getTime() / 1000;

gamesToMigrate.forEach(function (game) {
  var prev = '',
    uids = [];
  game.p.forEach(function (p) {
    if (p.uid && p.uid != prev) {
      uids.push(p.uid);
      prev = p.uid;
    }
  });

  var gameSris = game.uids || [];
  if (gameSris.length != uids.length) {
    ++j;
    db.game4.update({ _id: game['_id'] }, { $set: { uids: uids } });
  }

  ++it;
  if (it % batchSize == 0) {
    var percent = Math.round((it / max) * 100);
    var dat2 = new Date().getTime() / 1000;
    var perSec = Math.round(batchSize / (dat2 - dat));
    dat = dat2;
    print(it / 1000 + 'k ' + percent + '% ' + perSec + '/s - ' + j + ' updated');
    j = 0;
  }
});
