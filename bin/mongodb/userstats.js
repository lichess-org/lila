var games = db.game5;
var users = db.user4;

var batchSize = 1000;
var i,
  t,
  timeStrings,
  times,
  it = 0;
var dat = new Date().getTime() / 1000;
var max = users.count();

function hintWid(query) {
  return games.find(query).hint({ wid: 1 }).length();
}

print('Denormalize counts');
users.find().forEach(function (user) {
  var uid = user._id;
  var count = {
    game: games.count({ us: uid }),
    win: games.count({ wid: uid }),
    loss: games.count({ us: uid, s: { $in: [30, 31, 35, 33] }, wid: { $ne: uid } }),
    draw: games.count({ us: uid, s: { $in: [34, 32] } }),
    winH: hintWid({ wid: uid, 'p.ai': { $exists: false } }),
    lossH: games.count({
      us: uid,
      s: { $in: [30, 31, 35, 33] },
      wid: { $ne: uid },
      'p.ai': { $exists: false },
    }),
    drawH: games.count({ us: uid, s: { $in: [34, 32] }, 'p.ai': { $exists: false } }),
    ai: games.count({ us: uid, $or: [{ 'p0.ai': { $exists: true } }, { 'p1.ai': { $exists: true } }] }),
    rated: games.count({ us: uid, ra: true }),
  };
  users.update({ _id: uid }, { $set: { count: count } });
  ++it;
  if (it % batchSize === 0) {
    var percent = Math.round((it / max) * 100);
    var dat2 = new Date().getTime() / 1000;
    var perSec = Math.round(batchSize / (dat2 - dat));
    dat = dat2;
    print(it / 1000 + 'k ' + percent + '% ' + perSec + '/s');
  }
});
