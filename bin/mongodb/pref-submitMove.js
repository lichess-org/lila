const convert = s => (s == 1 ? 3 : s == 2 ? 31 : s == 4 ? 2 : 0);

const updateSubmitMove = pref =>
  db.pref.updateOne({ _id: pref._id }, { $set: { submitMove: convert(pref.submitMove) } });

db.pref.find({ submitMove: { $exists: 1 } }, { submitMove: 1 }).forEach(updateSubmitMove);
