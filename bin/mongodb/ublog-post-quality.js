db.ublog_post.find({ automod: { $exists: 1 } }).forEach(p => {
  db.ublog_post.updateOne(
    { _id: p._id },
    { $set: { quality: p.quality ?? p.modQuality ?? p.automod.quality ?? 0 } },
  );
});

db.ublog_post.updateMany({ approval: { $exists: false } }, { $set: { approval: 'verified' } });

db.ublog_post.updateMany({ modQuality: { $exists: true } }, { $unset: { modQuality: '' } });

db.ublog_post.updateMany(
  { quality: { $gt: 0 }, listedAt: { $exists: false }, 'lived.at': { $exists: true } },
  [{ $set: { listedAt: '$lived.at' } }],
);
