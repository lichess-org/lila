// db.puzzle_round2.drop();

let shift = 60000,
  o = {},
  m = 0,
  n = 0,
  total = db.puzzle_round.count();

const noWin = (1 << 30) - 1;

Number.prototype.pad = function (size) {
  let s = String(this);
  while (s.length < (size || 2)) {
    s = '0' + s;
  }
  return s;
};

db.puzzle_round
  .find()
  .sort({ $natural: -1 })
  .forEach(r => {
    if (r.p < shift) return;
    m = r.w ? 1 << 31 : 0;
    m += Math.abs(r.d || 0) << 16;
    m += r.r;
    o = {
      _id: `${r.u}:${(r.p - shift).pad(5)}`,
      a: r.a,
      m: NumberInt(m),
    };
    const win = !!(m >>> 31);
    const rating = (m << 16) >>> 16;
    const diff = ((m & noWin) >> 16) * (win ? 1 : -1);
    // printjson(r);
    // printjson(o);
    // print(win, rating, diff);
    try {
      if (win !== r.w) throw 'bad win';
      if (rating !== r.r) throw 'bad rating';
      if (diff !== r.d) throw 'bad diff';
      db.puzzle_round2.insert(o);
    } catch (e) {
      print(e, r._id);
    }
    n++;
    if (n % 10000 === 0) print(`${n}/${total}`);
  });
