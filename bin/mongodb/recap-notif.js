const year = 2024;
const dry = false;

let countAll = 0;
let countSent = 0;

print('Loading existing recaps...');
const hasRecap = new Set();
db.recap_report.find({}, { _id: 1 }).forEach(r => hasRecap.add(r._id));
print('Loaded ' + hasRecap.size + ' recaps');

const hasPuzzles = userId => db.user_perf.countDocuments({ _id: userId, 'puzzle.nb': { $gt: 0 } }, { limit: 1 });

// only keeps users that don't yet have a recap notification for the year
// and don't have yet loaded their recap from another link
const filterNewUsers = users => {
  const noRecap = users.filter(u => !hasRecap.has(u._id));
  const hasNotif = new Set(db.notify.distinct('notifies', {
    notifies: { $in: noRecap.map(u => u._id) }, 'content.type': 'recap', 'content.year': year
  }));
  return noRecap.filter(u => !hasNotif.has(u._id));
}

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
};

function sendToUser(user) {
  if (!user.count?.game && !hasPuzzles(user._id)) return;
  if (!dry) db.notify.insertOne({
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
      print('+ ' + newUsers.length + ' = ' + countSent + ' / ' + countAll);
      sleep(20 * newUsers.length);
    }
  }
  db.user4.find({
    enabled: true,
    seenAt: {
      // $gt: new Date('2024-01-01'),
      $gt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 2),
      $lt: new Date(Date.now() - 1000 * 60 * 20) // avoid the lila notif cache!
    }
  }).forEach(process);
  process(); // flush the generator
}

sendToRandomOfflinePlayers();

print('Scan: ' + countAll);
print('Sent: ' + countSent);
