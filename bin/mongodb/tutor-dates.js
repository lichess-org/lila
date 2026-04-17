const printDate = d => d.toISOString().split('T')[0];
db.tutor_report.find({ config: { $exists: 0 } }).forEach(old => {
  let minDate = new Date();
  let maxDate = new Date('2023-01-01');
  old.perfs.forEach(perf => {
    const dates = perf.stats.dates;
    if (dates[0] < minDate) minDate = dates[0];
    if (dates[1] > maxDate) maxDate = dates[1];
  });
  const report = {
    ...old,
    _id: `${old.user}:${printDate(minDate)}_${printDate(maxDate)}`,
    config: {
      user: old.user,
      from: minDate,
      to: maxDate,
    },
  };
  delete report.user;
  db.tutor_report.insertOne(report);
  db.tutor_report.deleteOne({ _id: old._id });
});
