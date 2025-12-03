db.fide_player_follower.find().forEach(function (doc) {
  db.fide_player_follower.updateOne(
    { _id: doc._id },
    { $set: { p: NumberInt(parseInt(doc._id.split('/')[0])) } },
  );
});
