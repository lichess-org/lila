var user = 'thibault';
// var kind = 'moderator';
var kind = 'marathonSurvivor';

db.trophy.insert({
  _id: kind + '/' + user,
  user: user,
  kind: kind,
  date: new Date(),
});
