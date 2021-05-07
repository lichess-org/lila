db.report2.drop();

db.report
  .aggregate(
    [
      {
        $group: {
          _id: {
            user: '$user',
            reason: '$reason',
            processedBy: '$processedBy',
          },
          reports: { $push: '$$ROOT' },
        },
      },
    ],
    { allowDiskUse: true }
  )
  .toArray()
  .forEach(group => {
    var reports = group.reports;
    var first = reports[0];

    var atoms = [];
    reports.forEach(r => {
      var same = atoms.find(a => a.by === r.createdBy);
      if (same) same.text += '\n\n' + r.text;
      else
        atoms.push({
          by: r.createdBy,
          at: r.createdAt,
          text: r.text,
          score: 30,
        });
    });

    var report = {
      _id: first._id,
      user: first.user,
      reason: first.reason,
      room: first.room,
      atoms: atoms,
      score: atoms.reduce((acc, atom) => acc + atom.score, 0),
      open: !first.processedBy,
    };
    if (first.processedBy) report.processedBy = first.processedBy;

    db.report2.insert(report);
  });

db.report2.createIndex({ room: 1, score: -1 }, { partialFilterExpression: { open: true }, name: 'best_open' });
db.report2.createIndex({ 'inquiry.mod': 1 }, { partialFilterExpression: { 'inquiry.mod': { $exists: true } } });
db.report2.createIndex({ user: 1 });
db.report2.createIndex({ 'atoms.by': 1 });
