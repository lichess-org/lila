print('Remove requests from closed teams, for https://github.com/lichess-org/lila/issues/10516');

agg = db.team_request.aggregate([
  { $group: { _id: '$team', reqIds: { $addToSet: '$_id' } } },
  {
    $lookup: {
      from: 'team',
      as: 'team',
      let: { teamId: '$_id' },
      pipeline: [
        { $match: { $expr: { $eq: ['$_id', '$$teamId'] }, enabled: false } },
        { $project: { _id: 1 } },
      ],
    },
  },
  { $unwind: { path: '$team' } },
  { $project: { reqIds: true, _id: false } },
  { $unwind: { path: '$reqIds' } },
  { $group: { _id: null, ids: { $push: '$reqIds' } } },
]);

if (agg.hasNext()) {
  reqIds = agg.next().ids;
  print(reqIds.length + ' requests to delete');
  db.team_request.deleteMany({ _id: { $in: reqIds } });
} else print('Nothing to do');
