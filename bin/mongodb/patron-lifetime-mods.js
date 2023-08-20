db.user4
  .find(
    {
      enabled: true,
      roles: { $in: ['ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_HUNTER', 'ROLE_SEE_REPORT'] },
    },
    { _id: 1, plan: 1 },
  )
  .forEach(user => {
    var userId = user._id;
    var prev = user.plan || {};

    print(userId, !!prev.active);

    db.user4.update(
      { _id: userId },
      {
        $set: {
          plan: {
            months: NumberInt(prev.months || 1),
            active: true,
            since: prev.since || new Date(),
          },
        },
      },
    );

    db.plan_patron.update(
      { _id: userId },
      {
        lastLevelUp: new Date(),
        lifetime: true,
      },
      { upsert: true },
    );
  });
