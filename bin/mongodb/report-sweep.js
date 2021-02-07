db.report
  .find({
    processedBy: {
      $exists: false,
    },
  })
  .sort({
    createdAt: -1,
  })
  .skip(150)
  .limit(1)
  .toArray()
  .forEach(function (report) {
    db.report.update(
      {
        processedBy: {
          $exists: false,
        },
        createdAt: {
          $gt: report.createdAt,
        },
      },
      {
        $set: {
          processedBy: 'lichess-sweep',
        },
      },
      {
        multi: true,
      }
    );
  });
