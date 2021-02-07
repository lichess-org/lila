var games = db.game5;
db.analysis2.find({}, { _id: true }).forEach(function (analysis) {
  games.update({ _id: analysis._id }, { $set: { an: true } });
});
