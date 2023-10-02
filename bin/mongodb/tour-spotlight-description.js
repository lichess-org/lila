db.tournament2.find({ 'spotlight.description': { $exists: 1 } }).forEach(t =>
  db.tournament2.updateOne(
    { _id: t._id },
    {
      $set: { description: t.description || t.spotlight.description },
      $unset: { 'spotlight.description': 1 },
    },
  ),
);
