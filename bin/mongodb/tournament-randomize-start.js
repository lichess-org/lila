db.tournament2
  .find(
    {
      startsAt: { $gt: new Date() },
    },
    {
      id: 1,
      startsAt: 1,
    },
  )
  .forEach(d => {
    if (d.startsAt.getSeconds() === 0) {
      printjson(d);
      d.startsAt.setSeconds(Math.floor(Math.random() * 59 + 1));
      db.tournament2.update(
        { _id: d._id },
        {
          $set: {
            startsAt: d.startsAt,
          },
        },
      );
    }
  });
