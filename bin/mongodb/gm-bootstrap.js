const userIds = ['sasha'].map(u => u.toLowerCase());
const perfTypes = ['bullet', 'blitz', 'rapid', 'classical'];
const perf = {
  gl: {
    r: 2700,
    d: 150,
    v: 0.06,
  },
  nb: NumberInt(0),
  re: [],
};
const updateTournaments = false;

userIds.forEach(id => {
  const user = db.user4.findOne({ _id: id });
  perfTypes.forEach(pt => {
    if (user && (!user.perfs[pt] || !user.perfs[pt].nb))
      db.user4.update({ _id: id }, { $set: { ['perfs.' + pt]: perf } });
  });
  if (updateTournaments)
    db.tournament_player.update(
      { uid: id, r: 1500, m: 1500 },
      { $set: { r: NumberInt(perf.gl.r), m: NumberInt(perf.gl.r) } },
      { multi: 1 },
    );
});
