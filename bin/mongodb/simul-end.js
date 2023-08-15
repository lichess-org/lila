var ids = [];

db.simul
  .find({
    status: 20,
  })
  .forEach(function (s) {
    var finished = true;
    printjson(s);
    s.pairings.forEach(function (p) {
      if (p.status < 25 && p.status !== 10) finished = false;
    });
    if (finished) ids.push(s._id);
  });

db.simul.update(
  {
    _id: {
      $in: ids,
    },
  },
  {
    $set: {
      status: NumberInt(30),
      finishedAt: new Date(),
      hostGameId: null,
    },
  },
  {
    multi: 1,
  },
);
