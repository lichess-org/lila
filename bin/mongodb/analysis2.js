var gamesToMigrate = db.analysis.find();
var max = gamesToMigrate.count();
var batchSize = 1000;
var collection = db.analysis2;

var piotr = {
  a: 'A1',
  b: 'B1',
  c: 'C1',
  d: 'D1',
  e: 'E1',
  f: 'F1',
  g: 'G1',
  h: 'H1',
  i: 'A2',
  j: 'B2',
  k: 'C2',
  l: 'D2',
  m: 'E2',
  n: 'F2',
  o: 'G2',
  p: 'H2',
  q: 'A3',
  r: 'B3',
  s: 'C3',
  t: 'D3',
  u: 'E3',
  v: 'F3',
  w: 'G3',
  x: 'H3',
  y: 'A4',
  z: 'B4',
  A: 'C4',
  B: 'D4',
  C: 'E4',
  D: 'F4',
  E: 'G4',
  F: 'H4',
  G: 'A5',
  H: 'B5',
  I: 'C5',
  J: 'D5',
  K: 'E5',
  L: 'F5',
  M: 'G5',
  N: 'H5',
  O: 'A6',
  P: 'B6',
  Q: 'C6',
  R: 'D6',
  S: 'E6',
  T: 'F6',
  U: 'G6',
  V: 'H6',
  W: 'A7',
  X: 'B7',
  Y: 'C7',
  Z: 'D7',
  0: 'E7',
  1: 'F7',
  2: 'G7',
  3: 'H7',
  4: 'A8',
  5: 'B8',
  6: 'C8',
  7: 'D8',
  8: 'E8',
  9: 'F8',
  '!': 'G8',
  '?': 'H8',
};

function decodePiotr(pp) {
  return pp == '_' ? null : (piotr[pp[0]] + piotr[pp[1]]).toLowerCase();
}

function decodeScore(pp) {
  return pp == '_' ? null : parseInt(pp);
}

print('Migrating ' + max + ' analysis');

collection.drop();

var nb = 0,
  dat = new Date().getTime() / 1000;
gamesToMigrate.forEach(function (a) {
  var encoded = a.encoded;
  if (!encoded) return;
  if (typeof encoded == 'undefined') return;
  try {
    var splitted = encoded.split(' ');
    var data = [];
    for (it = 0, l = splitted.length - 1; it < l; it++) {
      var cur = splitted[it].split(',');
      var next = splitted[it + 1].split(',');
      var move = decodePiotr(cur[0]),
        best = decodePiotr(cur[1]),
        score = decodeScore(next[2]),
        mate = decodeScore(next[3]);
      data.push([score, mate == null ? null : it % 2 == 1 ? mate : -mate].join(','));
    }
    a.data = data.join(';');
    delete a.encoded;
    a.old = true;
    // print(encoded);
    // print('--');
    // printjson(a);
    collection.insert(a);
  } catch (e) {
    printjson(a);
    print(e);
  }
  ++nb;
  if (nb % batchSize == 0) {
    var percent = Math.round((nb / max) * 100);
    var dat2 = new Date().getTime() / 1000;
    var perSec = Math.round(batchSize / (dat2 - dat));
    dat = dat2;
    print(nb / 1000 + 'k ' + percent + '% ' + perSec + '/s');
  }
});

print('Building indexes');
collection.ensureIndex({
  done: 1,
});
collection.ensureIndex({
  date: -1,
});
collection.ensureIndex({
  uid: 1,
});

print('Done!');
