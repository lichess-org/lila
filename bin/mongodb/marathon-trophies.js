const users = ['blitzstream-twitch', 'legend', 'admirala', 'hellball'];

for (let i of users) {
  const kind = i == 0 ? 'marathonWinner' : 'marathonTopTen';
  const user = users[i];
  db.trophy.insert({
    _id: kind + '/' + user,
    user: user,
    kind: kind,
    date: new Date(),
  });
}
