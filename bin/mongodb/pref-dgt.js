db.pref.updateMany({ 'tags.dgt': { $exists: true } }, { $set: { hasDgt: true } });
db.pref.updateMany({ tags: { $exists: 1, $ne: {} } }, { $unset: { tags: 1 } });
