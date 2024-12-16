const year = 2024;
const dry = false;

let count = 0;

const hasPuzzles = userId => db.user_perf.count({ _id: userId, 'puzzle.nb': { $gt: 0 } });

function sendTo(user) {
  const exists = db.notify.countDocuments({ notifies: user._id, 'content.type': 'recap', }, { limit: 1 });
  if (exists) {
    print('Already sent to ' + user._id);
    return;
  }
  if (!user.count?.game && !hasPuzzles(user._id)) {
    print('No games or puzzles for ' + user._id);
    return;
  }
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
  count++;
  print(count + ' ' + user._id);
}

function sendToRoleOwners() {
  db.user4.find({ enabled: true, roles: { $exists: 1, $ne: [] } }, { count: 1, roles: 1 }).forEach(user => {
    roles = user.roles.filter(r => r != 'ROLE_COACH' && r != 'ROLE_TEACHER' && r != 'ROLE_VERIFIED' && r != 'ROLE_BETA');
    if (roles.length) {
      sendTo(user);
    }
  });
}
