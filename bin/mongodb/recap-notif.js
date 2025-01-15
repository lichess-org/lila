const year = 2024;
const dry = false;

let countAll = 0;
let countSent = 0;
let lastPrinted = 0;

let hasRecap = new Set();
function reloadHasRecap() {
  print('Loading existing recaps...');
  hasRecap = new Set(db.recap_report.distinct('_id'));
  print('Loaded ' + hasRecap.size + ' recaps');
}
reloadHasRecap();
setInterval(reloadHasRecap, 1000 * 60 * 10);

const hasPuzzles = userId =>
  db.user_perf.countDocuments({ _id: userId, 'puzzle.nb': { $gt: 0 } }, { limit: 1 });

// only keeps users that don't yet have a recap notification for the year
// and don't have yet loaded their recap from another link
const filterNewUsers = users => {
  const noRecap = users.filter(u => !hasRecap.has(u._id));
  const hasNotif = new Set(
    db.notify.distinct('notifies', {
      notifies: { $in: noRecap.map(u => u._id) },
      'content.type': 'recap',
      'content.year': year,
    }),
  );
  return noRecap.filter(u => !hasNotif.has(u._id));
};

function* group(size) {
  let batch = [];
  while (true) {
    const element = yield;
    if (!element) {
      yield batch;
      return;
    }
    batch.push(element);
    if (batch.length >= size) {
      let element = yield batch;
      batch = [element];
    }
  }
}

function sendToUser(user) {
  if (!user.count?.game && !hasPuzzles(user._id)) return;
  if (!dry)
    db.notify.insertOne({
      _id: Math.random().toString(36).substring(2, 10),
      notifies: user._id,
      content: {
        type: 'recap',
        year: NumberInt(year),
      },
      read: false,
      createdAt: new Date(),
    });
  countSent++;
}

function sendToRandomOfflinePlayers() {
  const grouper = group(100);
  grouper.next();
  const process = user => {
    countAll++;
    const batch = grouper.next(user).value;
    if (batch) {
      const newUsers = filterNewUsers(batch);
      newUsers.forEach(sendToUser);
      if (countAll % 1000 == 0) {
        print(
          `+ ${countSent - lastPrinted} = ${countSent} / ${countAll} | ${user.createdAt.toLocaleDateString('fr')}`,
        );
        lastPrinted = countSent;
      }
      sleep(10 * newUsers.length);
    }
  };
  db.user4
    .find({
      enabled: true,
      createdAt: { $lt: new Date(year, 9, 1) },
      seenAt: {
        $gt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 30 * 3),
        // $lt: new Date(Date.now() - 1000 * 60 * 20) // avoid the lila notif cache!
      },
      marks: { $nin: ['boost', 'engine', 'troll'] },
    })
    .forEach(process);
  process(); // flush the generator
}

sendToRandomOfflinePlayers();

print('Scan: ' + countAll);
print('Sent: ' + countSent);
