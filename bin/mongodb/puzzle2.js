db.puzzle2.drop();
db.puzzle.find().forEach(function(o) {
  delete o.users;
  db.puzzle2.insert(o);
});
db.puzzle2.ensureIndex({'vote.sum':-1,'perf.gl.r':1});
