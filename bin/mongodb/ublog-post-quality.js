db.ublog_post.find({ automod: { $exists: 1 } }).forEach(p => {
  const set = {
    quality: p.automod.quality || 0,
  };
  if (p.automod.lockedBy) set['modQuality'] = set.quality;
  db.ublog_post.updateOne({ _id: p._id }, { $set: set });
});
