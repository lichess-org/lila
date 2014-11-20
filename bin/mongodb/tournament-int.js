function toInt(obj) {
  return function(prop) {
    if (typeof obj[prop] != 'undefined') obj[prop] = NumberInt(obj[prop]);
  };
}
// db.tournament.find({_id:'fefNHKaL'}).forEach(function(tour) {
db.tournament.find().sort({createdAt: -1}).forEach(function(tour) {
  ['status', 'mode', 'variant', 'system', 'minutes', 'minPlayers'].forEach(toInt(tour));
  if (tour.pairings) tour.pairings.forEach(function(pairing) {
    ['s', 't'].forEach(toInt(pairing));
  });
  if (tour.players) tour.players.forEach(function(player) {
    ['rating', 'score'].forEach(toInt(player));
  });
  if (tour.events) tour.events.forEach(function(event) {
    toInt(event)('i');
  });
  if (tour.clock) ['limit', 'increment'].forEach(toInt(tour.clock));
  db.tournament.update({"_id": tour._id}, tour);
});
