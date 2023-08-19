db.tournament2.createIndex(
  { 'conditions.teamMember.teamId': 1, startsAt: -1 },
  { partialFilterExpression: { 'conditions.teamMember': { $exists: 1 } } },
);
db.tournament2.createIndex(
  { 'teamBattle.teams': 1, startsAt: -1 },
  { partialFilterExpression: { teamBattle: { $exists: 1 } } },
);
db.tournament_player.createIndex(
  { tid: 1, t: 1, m: -1 },
  { partialFilterExpression: { t: { $exists: true } } },
  { background: true },
);
