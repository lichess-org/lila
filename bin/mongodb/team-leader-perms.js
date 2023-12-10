const perms = ['public', 'settings', 'tour', 'comm', 'request', 'pmall', 'kick', 'admin'];

db.team.find().forEach(function (team) {
  team.leaders.forEach(function (leader) {
    db.team_member.updateOne({ _id: `${leader}@${team._id}` }, { $set: { perms } });
  });
});
