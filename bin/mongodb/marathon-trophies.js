var users = [
  'blitzstream-twitch',
  'legend',
  'admirala',
  'hellball'
];

for (var i in users) {
  var kind = i == 0 ? 'marathonWinner' : 'marathonTopTen';
  var user = users[i];
  db.trophy.insert({
    _id: kind + '/' + user,
    user: user,
    kind: kind,
    date: new Date()
  });
}
