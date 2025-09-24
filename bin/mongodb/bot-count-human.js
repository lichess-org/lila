const botIds = new Set(db.user4.distinct('_id', { title: 'BOT', 'count.game': { $gte: 10 } }))

const countVsHuman = (uid) => {
  let nb = 0;
  db.game5.find({ us: uid }, { us: 1, t: 1 }).forEach(g => {
    const oppId = g.us.find(id => id && id !== uid);
    if (oppId && !botIds.has(oppId)) nb += g.t
  })
  return nb;
}

db.user4.find({ title: 'BOT' }, { username: 1 }).forEach(function(user) {
  const count = countVsHuman(user._id);
  if (count > 0) print(user.username + ' ' + count);
  db.user4.updateOne({ _id: user._id }, { $set: { 'time.human': count } });
});

