db.report2.update({ room: 'coms' }, { $set: { room: 'comm' } }, { multi: 1 });

db.report2.distinct('user', { room: 'comm' }).forEach(user => {
  const reports = db.report2.find({ user: user, room: 'comm' }).toArray();

  const report = reports[0];
  const others = reports.slice(1);
  report.reason = 'comm';

  others.forEach(rep => {
    report.atoms = report.atoms.concat(rep.atoms);
    report.score = report.score + rep.score;
    report.open = report.open && rep.open;
    report.processedBy = report.processedBy || rep.processedBy;
  });

  if (!report.processedBy || report.open) delete report.processedBy;
  report.atoms.sort((a, b) => a.at > b.at);

  db.report2.update({ _id: report._id }, { $set: report });

  if (others.length) db.report2.remove({ _id: { $in: others.map(r => r._id) } });
});
