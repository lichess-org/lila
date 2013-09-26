var d = new Date();
d.setDate(d.getDate()-1);

var ids = db.game4.find({t:{$lt:2},ca:{$lt:d}}, {_id:1}).limit(1000).toArray().map(function(g) {
  return g._id;
});
var nb = ids.length;

if (nb > 0) {
  print("First ID: " + ids[0]);
}

print("Remove " + nb + " games");
db.game4.remove({_id:{$in:ids}});

print("Remove " + nb + " pgns");
db.pgn.remove({_id:{$in:ids}});

print("Remove " + nb + " bookmarks");
db.bookmark.remove({g:{$in:ids}});
