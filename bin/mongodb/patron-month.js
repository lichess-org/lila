var userId = 'neio';

var user = db.user4.findOne({_id:userId});

db.plan_patron.update({
  _id:'neio'
}, {
  lastLevelUp: new Date(),
  expiresAt: new Date(Date.now() + 1000 * 3600 * 24 * 31)
}, {upsert:true});

db.user4.update({_id:userId}, {
  $set: {
    plan: {
      months: NumberInt((user.plan ? user.plan.months : 0) + 1),
      active: true,
      since: user.plan ? user.plan.since : new Date()
    }
  }
});
