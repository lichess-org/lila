db.plan_patron.find({ lifetime: true }).forEach(plan => {
  db.user4.updateOne({ _id: plan._id }, { $set: { 'plan.lifetime': true } });
});

// not quite yet
// db.plan_patron.updateMany({ lifetime: { $exists: true } }, { $unset: { lifetime: true } });
