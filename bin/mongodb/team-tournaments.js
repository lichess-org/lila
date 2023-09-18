db.tournament2.dropIndex('teamBattle.teams_1_startsAt_-1');
db.tournament2.dropIndex('conditions.teamMember.teamId_1_startsAt_-1');
db.tournament2.ensureIndex(
  { forTeams: 1, startsAt: -1 },
  { partialFilterExpression: { forTeams: { $exists: 1 } } },
);

db.tournament2
  .find({ teamBattle: { $exists: 1 } })
  .forEach(t => db.tournament2.update({ _id: t._id }, { $set: { forTeams: t.teamBattle.teams } }));

db.tournament2
  .find({ 'conditions.teamMember.teamId': { $exists: 1 } })
  .forEach(t =>
    db.tournament2.update({ _id: t._id }, { $set: { forTeams: [t.conditions.teamMember.teamId] } }),
  );
