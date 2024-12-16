const year = 2024;
const dry = false;

let count = 0;

const hasPuzzles = userId => db.user_perf.count({ _id: userId, 'puzzle.nb': { $gt: 0 } });

function sendToUser(user) {
  if (!user.enabled) {
    print('------------- ' + user._id + ' is closed');
    return;
  }
  const exists = db.notify.countDocuments({ notifies: user._id, 'content.type': 'recap', }, { limit: 1 });
  if (exists) {
    print('------------- ' + user._id + ' already sent');
    return;
  }
  if (user.seenAt < new Date('2024-01-01')) {
    print('------------- ' + user._id + ' not seen in 2024');
    return;
  }
  if (!user.count?.game && !hasPuzzles(user._id)) {
    print('------------- ' + user._id + ' no games or puzzles');
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

function sendToUserId(userId) {
  const user = db.user4.findOne({ _id: userId });
  if (!user) {
    print('------------- ' + userId + ' not found');
    return;
  }
  sendToUser(user);
}

function sendToRoleOwners() {
  db.user4.find({ enabled: true, roles: { $exists: 1, $ne: [] } }).forEach(user => {
    roles = user.roles.filter(r => r != 'ROLE_COACH' && r != 'ROLE_TEACHER' && r != 'ROLE_VERIFIED' && r != 'ROLE_BETA');
    if (roles.length) {
      sendTo(user);
    }
  });
}

function sendToTeamMembers(teamId) {
  db.team_member.find({ team: teamId }, { user: 1, _id: 0 }).forEach(member => {
    sendToUserId(member.user);
  });
}

function sendToRandomOnlinePlayers() {
  db.user4.find({ enabled: true, 'count.game': { $gt: 10 }, seenAt: { $gt: new Date(Date.now() - 1000 * 60 * 2) } }).sort({ seenAt: -1 }).limit(5_000).forEach(sendToUser);
}

function sendToRandomOfflinePlayers() {
  db.user4.find({
    enabled: true, 'count.game': { $gt: 10 }, seenAt: {
      $gt: new Date(Date.now() - 1000 * 60 * 60 * 24),
      $lt: new Date(Date.now() - 1000 * 60 * 60)
    }
  }).limit(25_000).forEach(sendToUser);
}

sendToRandomOfflinePlayers();
