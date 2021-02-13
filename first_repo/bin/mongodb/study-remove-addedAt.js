db.study.find({}, { members: 1 }).forEach(s => {
  for (let i in s.members) {
    delete s.members[i].addedAt;
  }

  db.study.update({ _id: s._id }, { $set: { members: s.members } });
});
