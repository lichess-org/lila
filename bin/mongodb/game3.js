var gamesToMigrate = db.game2.find();
var max = gamesToMigrate.count();
var batchSize = 10000;
var collection = db.game3;
var pgnCollection = db.pgn;

print("Migrating " + max + " games");

collection.drop();
pgnCollection.drop();

function rename(arr, from, to) {
  if (typeof arr[from] !== 'undefined') {
    arr[to] = arr[from];
    delete arr[from];
  }
}

function map(arr, key, f) {
  if (typeof arr[key] !== 'undefined') {
    arr[key] = f(arr[key]);
  }
}

function clean(arr, key) {
  if (typeof arr[key] !== 'undefined' && (arr[key] === null || arr[key] === "")) {
    delete arr[key];
  }
}

function cleanArray(arr, key) {
  if (typeof arr[key] !== 'undefined' && arr[key].length === 0) {
    delete arr[key];
  }
}

function cleanOrRename(arr, from, to) {
  clean(arr, from);
  rename(arr, from, to);
}

function removeSpace(str) { return str.replace(/ /g, ''); }

function finishedOrAborted(game) { return game.status >= 25; }

var c, z, pgn;
var it = 0;
var dat = new Date().getTime() / 1000;
var finishedPlayerFieldsToRemove = ['previousMoveTs', 'lastDrawOffer', 'isOfferingDraw', 'isProposingTakeback'];

gamesToMigrate.forEach(function(g) {
  cleanOrRename(g, 'castles', 'cs');
  if (g.cc === 'white') { 
    delete g.cc;
  } else {
    g.cc = false;
  }
  cleanOrRename(g, 'check', 'ck');
  cleanOrRename(g, 'lastMove', 'lm');
  map(g, 'lm', removeSpace);
  cleanOrRename(g, 'initialFen', 'if');
  clean(g, 'clock');
  cleanArray(g, 'userIds');
  rename(g, 'userIds', 'uids');
  rename(g, 'createdAt', 'ca');
  rename(g, 'turns', 't');
  rename(g, 'status', 's');
  cleanOrRename(g, 'updatedAt', 'ua');
  cleanOrRename(g, 'winId', 'wid');
  delete g.r960;
  if (typeof g.v !== 'undefined' && g.v != 2) {
    delete g.v;
  }
  if (typeof g.isRated != 'undefined' && g.isRated !== true) {
    delete g.isRated;
  } else {
    cleanOrRename(g, 'isRated', 'ra');
  }
  if (finishedOrAborted(g)) {
    delete g.positionHashes;
    delete g.lmt;
  } else {
    cleanOrRename(g, 'positionHashes', 'ph');
  }
  cleanOrRename(g, 'clock', 'c');
  if (c = g.c) {
    if (finishedOrAborted(g)) {
      delete c.timer;
    } else {
      cleanOrRename(c, 'timer', 't');
    }
    if (typeof c.c !== 'undefined') {
      c.c = c.c === 'white';
    }
  }
  rename(g, 'players', 'p');
  g.p.forEach(function(p) {
    delete p.c;
    map(p, 'ps', removeSpace);
    if (finishedOrAborted(g) && typeof p.blurs !== 'undefined' && p.blurs < 7) {
      delete p.blurs;
    } else {
      cleanOrRename(p, 'blurs', 'bs');
    }
    if (finishedOrAborted(g)) {
      for (z in finishedPlayerFieldsToRemove) {
        delete p[z];
      }
    }
    delete p.isAi;
    rename(p, 'aiLevel', 'ai');
  });
  pgn = g.pgn;
  delete g.pgn;
  collection.insert(g);
  if (pgn !== null && pgn !== "") {
    pgnCollection.insert({_id: g._id, p: pgn});
  }
  ++it;
  if (it % batchSize == 0) {
    var percent = Math.round((it / max) * 100);
    var dat2 = new Date().getTime() / 1000;
    var perSec = Math.round(batchSize / (dat2 - dat));
    dat = dat2;
    print((it / 1000) + "k " + percent + "% " + perSec + "/s");
  }
});

print("Building indexes");
collection.ensureIndex({s: 1});
collection.ensureIndex({uids: 1}, {sparse: 1});
collection.ensureIndex({wid: 1}, {sparse: 1});
collection.ensureIndex({ca: -1});
collection.ensureIndex({uids: 1, ca: -1});
collection.ensureIndex({bm: 1}, {sparse: 1});

print("Done!");
