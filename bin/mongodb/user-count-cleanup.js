let i = 0;
db.user4
  .find(
    {
      $or: [
        { 'count.ai': { $exists: true } },
        { 'count.lossH': { $exists: true } },
        { 'count.drawH': { $exists: true } },
        { 'count.winH': { $exists: true } },
      ],
    },
    { _id: 1 },
  )
  .forEach(function (user) {
    db.user4.updateOne(
      { _id: user._id },
      {
        $unset: {
          'count.ai': true,
          'count.lossH': true,
          'count.drawH': true,
          'count.winH': true,
        },
      },
    );
    i++;
    if (i % 10000 === 0) print(i);
  });
