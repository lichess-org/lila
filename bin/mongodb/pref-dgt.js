db.pref.updateMany(
  { tags: { $exists: { dgt: { $exists: true } } } },
  { $set: { hasDgt: true } },
);
db.pref.updateMany({}, { $unset: { tags: "" } });
