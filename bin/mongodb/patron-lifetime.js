var userId = 'shork'.toLowerCase();

var prev = db.user4.findOne({ _id: userId }).plan;

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
