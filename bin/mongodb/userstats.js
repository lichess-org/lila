const games = db.game5;
const users = db.user4;

const batchSize = 1000;
let i,
  t,
  timeStrings,
  times,
  it = 0;
let dat = new Date().getTime() / 1000;
const max = users.count();

function hintWid(query) {
  return games.find(query).hint({ wid: 1 }).length();
}

print('Denormalize counts');
users.find().forEach(function (user) {
  const uid = user._id;
  const count = {
    game: games.count({ us: uid }),
    win: games.count({ wid: uid }),
    loss: games.count({ us: uid, s: { $in: [30, 31, 35, 33] }, wid: { $ne: uid } }),
    draw: games.count({ us: uid, s: { $in: [34, 32] } }),
    rated: games.count({ us: uid, ra: true }),
  };
  users.update({ _id: uid }, { $set: { count: count } });
  ++it;
  if (it % batchSize === 0) {
    const percent = Math.round((it / max) * 100);
    const dat2 = new Date().getTime() / 1000;
    const perSec = Math.round(batchSize / (dat2 - dat));
    dat = dat2;
    print(it / 1000 + 'k ' + percent + '% ' + perSec + '/s');
  }
});
