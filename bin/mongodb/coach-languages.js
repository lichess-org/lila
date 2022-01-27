db.coach.find({}).forEach(c => {
  let u = db.user4.findOne({ _id: c._id });
  u && db.coach.update({ _id: c._id }, { $set: { languages: u.lang ? [u.lang] : [] } });
});
