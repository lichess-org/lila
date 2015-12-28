db.tournament.aggregate({
  $match: {
    _id: "winter15"
  }
}, {
  $project: {
    'pairings.u': true,
    _id: false
  }
}, {
  $unwind: '$pairings'
}, {
  $unwind: '$pairings.u'
}, {
  $group: {
    _id: '$pairings.u',
    nb: {
      $sum: 1
    }
  }
}, {
  $match: {
    nb: {
      $gte: 5
    }
  }
}).result.forEach(function(r) {
  db.trophy.insert({
    _id: 'survivor2/' + r._id,
    kind: 'marathonSurvivor2',
    user: r._id,
    date: new Date()
  });
});
