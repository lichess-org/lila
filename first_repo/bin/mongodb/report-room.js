var lastWeek = new Date(Date.now() - 1000 * 60 * 60 * 24 * 7);

db.report.find().forEach(r => {
  var room = 'others';
  if (!r.processedBy && r.createdAt < lastWeek) room = 'xfiles';
  else if (r.reason === 'cheat') room = 'cheat';
  else if (r.reason === 'cheatprint') room = 'print';
  else if (r.reason === 'troll') room = 'coms';
  else if (r.reason === 'insult') room = 'coms';

  db.report.update({ _id: r._id }, { $set: { room: room } });
});
