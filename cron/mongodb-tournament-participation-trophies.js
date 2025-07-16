// run this every 30s for the duration of the tournament

const tourId = 'fscday25';

try {
  db.trophyKind.insertOne({
    _id: tourId,
    name: 'FIDE\'s Social Chess Day',
    order: NumberInt(102),
    withCustomImage: true
  });
} catch (e) {
  if (e.code !== 11000) throw e; // Ignore duplicate key error
}


const userIds = db.tournament_player.distinct('uid', {
  tid: tourId,
  s: { $gt: 0, $lt: 20 }
});

const now = new Date();

const existing = db.trophy.distinct('user', {
  kind: tourId,
  user: { $in: userIds }
});

const newUserIds = userIds.filter(uid => !existing.includes(uid));

console.log('Inserting trophies for', newUserIds.length, 'users');

const trophies = newUserIds.map(uid => ({
  _id: Math.random().toString(36).substring(2, 10),
  user: uid,
  kind: tourId,
  url: 'https://lichess.org/tournament/' + tourId,
  date: now
}));

if (trophies.length) {
  const res = db.trophy.insertMany(trophies, { ordered: false });
  console.log('Inserted new trophies for', Object.keys(res.insertedIds).length, ' users');
}

