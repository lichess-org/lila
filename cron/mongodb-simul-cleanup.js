const since = new Date(Date.now() - 1000 * 3600 * 24 * 7);
db.simul.deleteMany({
  status: 10,
  hostSeenAt: { $lt: since },
  $or: [{ estimatedStartAt: null }, { estimatedStartAt: { $lt: since } }],
});
