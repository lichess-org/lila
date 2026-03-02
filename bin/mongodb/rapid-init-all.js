// Courtesy of Issac Levy
function getTc(g) {
  if (g.c) {
    const h = g.c.hex();
    const lim = parseInt(h.substr(0, 2), 16);
    return [lim < 181 ? lim * 60 : (lim - 180) * 15, parseInt(h.substr(2, 2), 16)];
  }
}
function getPerf(tc) {
  if (!tc) return false;
  const totalTime = tc[0] + tc[1] * 40;
  if (totalTime < 480) return false;
  if (totalTime < 1500) return 'rapid';
  return 'classical';
}

function copyPerf(p) {
  return {
    gl: {
      r: p.gl.r,
      v: p.gl.v,
      d: p.gl.d,
    },
    nb: p.nb,
    re: p.re,
  };
}

const initialGlicko = {
  r: 1500,
  v: 0.06,
  d: 350,
};

// var query = {_id:{$in:['thibault', 'neio']}};
const query = {
  'perfs.classical.nb': { $gte: 1 },
  v: { $exists: false },
};

let done = 0;
db.user4.find(query).forEach(u => {
  if (u.v) return;
  print(done + ' ' + u._id + ' - ' + u.count.game + ' games');
  const classicalPerf = u.perfs.classical;
  classicalPerf.la = undefined;
  classicalPerf.nb = NumberInt(0);
  classicalPerf.re = [];
  const rapidPerf = copyPerf(classicalPerf);
  const gameQuery = {
    us: u._id,
    s: { $gte: 30 },
    ra: true,
    v: { $exists: false },
    c: { $exists: true },
  };
  db.game5
    .find(gameQuery, { _id: false, c: true, ca: true })
    .sort({ ca: -1 })
    .hint('us_1_ca_-1')
    .forEach(g => {
      switch (getPerf(getTc(g))) {
        case 'rapid':
          rapidPerf.nb++;
          if (!rapidPerf.la) rapidPerf.la = g.ca;
          break;
        case 'classical':
          classicalPerf.nb++;
          if (!classicalPerf.la) classicalPerf.la = g.ca;
          break;
      }
    });
  [classicalPerf, rapidPerf].forEach(perf => {
    if (perf.nb < 10) {
      if (perf.nb < 5) perf.gl.d = 350;
      else perf.gl.d = 150;
      perf.gl.v = 0.06;
    }
    if (!perf.la) delete perf.la;
    perf.nb = NumberInt(perf.nb);
  });

  const update = {
    $set: {
      v: 2, // user version 2
    },
  };

  if (classicalPerf.nb == 0) update['$unset'] = { 'perfs.classical': true };
  else update['$set']['perfs.classical'] = classicalPerf;
  if (rapidPerf.nb > 0) update['$set']['perfs.rapid'] = rapidPerf;

  db.user4.update({ _id: u._id }, update);
  db.perf_stat.remove({ _id: u._id + '/3' });
  done++;
});
