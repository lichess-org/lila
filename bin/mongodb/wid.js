var gamesToMigrate = db.game5.find(
  {
    w: {
      $exists: true,
    },
    wid: {
      $exists: false,
    },
    us: {
      $exists: true,
    },
    ca: {
      $gt: ISODate('2013-12-15T10:55:07.702Z'),
    },
  },
  {
    us: 1,
    w: 1,
  },
);

gamesToMigrate.forEach(function (g) {
  if (g.w && typeof g.us[0] != 'undefined' && g.us[0]) {
    db.game5.update(
      {
        _id: g._id,
      },
      {
        $set: {
          wid: g.us[0],
        },
      },
    );
  } else if (!g.w && typeof g.us[1] != 'undefined' && g.us[1]) {
    db.game5.update(
      {
        _id: g._id,
      },
      {
        $set: {
          wid: g.us[1],
        },
      },
    );
  }
});

print('Done!');
