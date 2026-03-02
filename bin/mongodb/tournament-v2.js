const orig = db.tournament;
const dest = db.tournament2;
const pairings = db.tournament_pairing;
const players = db.tournament_player;
const batchSize = 5;
const pause = 500;

// dest.drop();
// pairings.drop();
// players.drop();

dest.ensureIndex({
  status: 1,
});
dest.ensureIndex({
  createdAt: 1,
});
dest.ensureIndex({
  startsAt: 1,
});
pairings.ensureIndex({
  tid: 1,
  d: -1,
});
pairings.ensureIndex({
  tid: 1,
  u: 1,
  d: -1,
});
players.ensureIndex({
  tid: 1,
  uid: 1,
});
players.ensureIndex({
  tid: 1,
  m: -1,
});

const cursor = orig
  .find({
    status: 30,
  })
  .sort({
    createdAt: -1,
  }); //.limit(max);

function int(i) {
  return NumberInt(i);
}

let uidIt = 0;

function uid() {
  return 'i' + uidIt++;
}

let it = 0;
const max = cursor.count();
let dat = new Date().getTime() / 1000;

cursor.forEach(function (o) {
  dest.insert(mkTour(o));
  insertPlayers(o);
  insertPairings(o);

  ++it;
  if (it % batchSize == 0) {
    const percent = Math.round((it / max) * 100);
    const dat2 = new Date().getTime() / 1000;
    const ms = Math.round(1000 * (dat2 - dat - pause / 1000));
    const perSec = Math.round(batchSize / ms);
    dat = dat2;
    print(it + ' ' + percent + '% ' + ms + 'ms');
    sleep(pause);
  }
});

function insertPlayers(o) {
  const bulk = players.initializeUnorderedBulkOp();
  for (let i in o.players) {
    const p = o.players[i];
    const n = {
      _id: uid(),
      tid: o._id,
      uid: p.id,
      r: int(p.rating),
      m: int(p.score * 1000000 + (p.perf || 0) * 1000 + p.rating),
    };
    if (p.withdraw) n.w = true;
    if (p.prov) n.pr = true;
    if (p.score) n.s = int(p.score);
    if (p.perf) n.p = int(p.perf);
    bulk.insert(n);
  }
  bulk.execute();
}

function insertPairings(o) {
  const bulk = pairings.initializeUnorderedBulkOp();
  for (let i in o.pairings) {
    const p = o.pairings[i];
    const n = {
      _id: p.g,
      tid: o._id,
      s: int(p.s),
      u: p.u,
      t: int(p.t),
    };
    if (p.w) n.w = p.w == p.u[0];
    if (p.b1) n.b1 = int(p.b1);
    if (p.b2) n.b2 = int(p.b2);
    bulk.insert(n);
  }
  bulk.execute();
}

function mkTour(o) {
  const nbPlayers = o.players.length;
  const n = {
    _id: o._id,
    name: o.name,
    status: int(o.status),
    clock: o.clock,
    minutes: int(o.minutes),
    nbPlayers: int(nbPlayers),
    createdAt: o.createdAt,
    createdBy: o.createdBy,
    startsAt: o.schedule ? o.schedule.at : o.createdAt,
  };
  if (o.system && o.system != 1) n.system = int(o.system);
  if (o.variant && o.variant != 1) n.variant = int(o.variant);
  if (o.mode && o.mode != 1) n.mode = int(o.mode);
  if (o.schedule)
    n.schedule = {
      freq: o.schedule.freq,
      speed: o.schedule.speed,
    };
  if (nbPlayers) n.winner = o.players[0].id;
  return n;
}
