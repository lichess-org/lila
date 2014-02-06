db.puzzle.find({
  perf: {
    $exists: false
  }
}).forEach(function(o) {
  db.puzzle.update({
    _id: o._id
  }, {
    $set: {
      perf: {
        gl: o.rating,
        nb: NumberInt(0)
      }
    },
    $unset: {
      rating: true
    }
  });
});
db.puzzle.find({
  'vote.up': {
    $exists: false
  }
}).forEach(function(o) {
  db.puzzle.update({
    _id: o._id
  }, {
    $set: {
      vote: {
        up: NumberInt(0),
        down: NumberInt(0),
        sum: NumberInt(0)
      }
    }
  });
});
