var gamesToMigrate = db.game5
  .find(
    {
      $or: [
        {
          'p0.w': true,
        },
        {
          'p1.w': true,
        },
      ],
    },
    {
      'p0.w': true,
    },
  )
  .sort({ ca: -1 });

gamesToMigrate.forEach(function (g) {
  var color = !!(g.p0 && g.p0.w);
  var update = {
    $unset: color
      ? {
          'p0.w': true,
        }
      : {
          'p1.w': true,
        },
    $set: {
      w: color,
    },
  };
  // printjson(g);
  // printjson(update);
  // print(' ');
  db.game5.update(
    {
      _id: g._id,
    },
    update,
  );
});

print('Unsetting empty players...');

db.game5.update({ p0: {} }, { $unset: { p0: 1 } }, { multi: true });
db.game5.update({ p1: {} }, { $unset: { p1: 1 } }, { multi: true });

print('Done!');
