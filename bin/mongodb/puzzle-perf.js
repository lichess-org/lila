db.puzzle.find().forEach(function(o) {
  db.puzzle.update({_id:o._id},{
    $set: {
      perf: {
        gl: o.rating,
        nb: 0
      }
    },
    $unset: {
      rating: true
    }
  });
});
