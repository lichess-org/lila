const user = 'thibault';
// var kind = 'moderator';
const kind = 'marathonSurvivor';

db.trophy.insert({
  _id: kind + '/' + user,
  user: user,
  kind: kind,
  date: new Date(),
});
