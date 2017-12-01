db.report.find({processedBy:{$exists:true}}).forEach(r => {

  r.atoms = [{
    by: r.createdBy,
    at: r.createdAt,
    text: r.text,
    score: 0
  }];
  r.score = 0;

  ['createdBy', 'createdBy', 'text'].forEach(field => {
    delete r[field];
  });

  db.report2.insert(r);
});

db.report.aggregate(
  {$match:{processedBy:{$exists:false}}},
  {$group:{_id:'$user',reports:{$push:'$$ROOT'}}}
).toArray().forEach(group => {

  var reports = group.reports;
  var first = reports[0];

  var report = {
    _id: first._id,
    user: first.user,
    reason: first.reason,
    room: first.room,
    atoms: reports.map(r => ({
      by: r.createdBy,
      at: r.createdAt,
      text: r.text,
      score: 30
    })),
    score: reports.length * 30
  };

  db.report2.insert(report);
});
