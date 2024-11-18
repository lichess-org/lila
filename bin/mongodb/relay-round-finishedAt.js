db.relay.find({ finished: true, finishedAt: { $exists: false } }).forEach(function(relay) {
  const startAt = relay.startedAt || relay.startsAt || relay.createdAt;
  const duration = 1000 * 60 * 60 * 3; // 3 hours
  const finishAt = new Date(startAt.getTime() + duration);
  db.relay.updateOne({ _id: relay._id }, { $set: { finishedAt: finishAt } });
});
