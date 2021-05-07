db.team.find().forEach(t => {
  if (t.leaders.length == 1 && t.leaders[0] == t.createdBy) return;
  const leaders = db.team_member.distinct('user', { team: t._id, user: { $in: t.leaders } });
  if (leaders.length != t.leaders.length) {
    if (!leaders.length) leaders.push(t.createdBy);
    print(`${t._id} ${t.name} ${t.leaders.join(',')} -> ${leaders.join(',')}`);
    db.team.update({ _id: t._id }, { $set: { leaders: leaders } });
  }
});
