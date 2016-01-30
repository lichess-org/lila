var id = 14065;
db.puzzle.update({_id:id},{$set:{perf:db.puzzle.findOne({_id:60002}).perf}});
