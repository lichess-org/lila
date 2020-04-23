db.team.find({},{createdBy:1}).forEach(t => {
  db.team.update({_id:t._id},{$set:{leaders:[t.createdBy]}});
});
db.team.ensureIndex({leaders:1},{background:1})
