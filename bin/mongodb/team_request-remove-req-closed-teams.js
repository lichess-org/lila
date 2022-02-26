print('Removing pending requests from closed teams, for https://github.com/lichess-org/lila/issues/10516');

const reqsClosedTeams = db.team_request.aggregate([
  { $group: { _id: '$team', req_ids: { $addToSet: '$_id' } } },
  {
    $lookup: {
      from: 'team',
      localField: '_id',
      foreignField: '_id',
      as: 'team',
      pipeline: [{ $match: { enabled: false } }, { $project: { enabled: '$enabled' } }],
    },
  },
  { $unwind: { path: '$team' } },
]);

// returns
// [
//   {
//     _id: 'closed-team-with-pending-requests',
//     req_ids: [
//       'test@closed-team-with-pending-requests',
//       'test2@closed-team-with-pending-requests'
//     ],
//     team: { _id: 'closed-team-with-pending-requests', enabled: false }
//   }
// ]
// each document listing the pending requests of closed teams

reqsClosedTeams.forEach(closedTeam => db.team_request.deleteMany({ _id: { $in: closedTeam.req_ids } }));
