const gamesToMigrate = db.game3.find();
const max = gamesToMigrate.count();
const batchSize = 10000;
const collection = db.game4;

print('Migrating ' + max + ' games');

collection.drop();

const timechars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
const timecharsLength = timechars.length;
const lastchar = timechars[timecharsLength - 1];

let i,
  t,
  timeStrings,
  times,
  it = 0;
let dat = new Date().getTime() / 1000;
gamesToMigrate.forEach(function (g) {
  g.p.forEach(function (p) {
    if (typeof p.mts != 'undefined') {
      if (p.mts === null || p.mts.length === 0) {
        delete p.mts;
      } else {
        try {
          times = '';
          timeStrings = p.mts.split(' ');
          for (i in timeStrings) {
            t = parseInt(timeStrings[i]);
            times += t < timecharsLength ? timechars[t] : lastchar;
          }
          p.mts = times;
        } catch (e) {
          print(g._id + ' ' + e);
          delete p.mts;
        }
      }
    }
  });
  collection.insert(g);
  ++it;
  if (it % batchSize == 0) {
    const percent = Math.round((it / max) * 100);
    const dat2 = new Date().getTime() / 1000;
    const perSec = Math.round(batchSize / (dat2 - dat));
    dat = dat2;
    print(it / 1000 + 'k ' + percent + '% ' + perSec + '/s');
  }
});

print('Building indexes');
collection.ensureIndex({ s: 1 });
collection.ensureIndex({ uids: 1 }, { sparse: 1 });
collection.ensureIndex({ wid: 1 }, { sparse: 1 });
collection.ensureIndex({ ca: -1 });
collection.ensureIndex({ uids: 1, ca: -1 });
collection.ensureIndex({ bm: 1 }, { sparse: 1 });

print('Done!');
