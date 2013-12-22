db.tournament.find({}, {players:1}).forEach(function(tour) {
  for (var i in tour.players) {
    var p = tour.players[i];
    p.rating = p.elo;
    delete p.elo;
    tour.players[i] = p;
  }
  db.tournament.update({"_id": tour._id}, {"$set":{players: tour.players}});
});
