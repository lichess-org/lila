var coll = db.puzzle;
coll
  .find({ _id: { $lt: 60120 }, 'vote.sum': { $gte: 100 } })
  .sort({ _id: 1 })
  .forEach(function (p) {
    if (coll.count({ fen: p.fen }) == 1) {
      var nextId = coll.find({}, { _id: 1 }).sort({ _id: -1 }).limit(1)[0]._id + 1;
      p.salvaged = NumberInt(p._id);
      p._id = NumberInt(nextId);
      print(p.salvaged + ' -> ' + p._id);
      coll.insert(p);
    }
  });
