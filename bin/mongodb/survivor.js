db.tournament
  .aggregate(
    {
      $match: {
        _id: 'eY3dydc7',
      },
    },
    {
      $project: {
        'pairings.u': true,
        _id: false,
      },
    },
    {
      $unwind: '$pairings',
    },
    {
      $unwind: '$pairings.u',
    },
    {
      $group: {
        _id: '$pairings.u',
        nb: {
          $sum: 1,
        },
      },
    },
    {
      $match: {
        nb: {
          $gte: 5,
        },
      },
    },
  )
  .result.forEach(function (r) {
    db.trophy.insert({
      _id: 'survivor/' + r._id,
      kind: 'marathonSurvivor',
      user: r._id,
      date: new Date(),
    });
  });
