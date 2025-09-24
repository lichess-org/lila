const botIds = new Set(db.user4.distinct('_id', { title: 'BOT', 'count.game': { $gte: 10 } }));

print(botIds.size + ' bot ids');

const timeVsHuman = uid => {
  let nb = 0;
  let seconds = 0;
  db.game5.find({ us: uid, c: { $exists: 1 } }, { us: 1, t: 1, ca: 1, ua: 1 }).forEach(g => {
    const oppId = g.us.find(id => id && id !== uid);
    if (oppId && !botIds.has(oppId)) {
      const s = Math.round(Math.min(g.t * 5, (g.ua - (g.ca | 0)) / 1000));
      seconds += s;
      nb++;
    }
  });
  return [nb, seconds];
};

db.user4.find({ title: 'BOT' }, { username: 1 }).forEach(function (user) {
  const [nb, seconds] = timeVsHuman(user._id);
  if (seconds > 0)
    print(user.username + ' ' + seconds + 's in ' + nb + ' games, ', Math.round(seconds / nb), 's/game');
  db.user4.updateOne({ _id: user._id }, { $set: { 'time.human': seconds } });
});
