db.streamer.update(
  {
    liveAt: { $exists: 0 },
    createdAt: { $lt: new Date(Date.now() - 1000 * 3600 * 24 * 7) },
  },
  {
    $set: {
      'approval.granted': false,
      demoted: true,
    },
  },
  {
    multi: 1,
  },
);
