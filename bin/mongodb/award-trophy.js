var user = 'thibault';
// var kind = 'moderator';
var kind = 'wayOfBerserk';

db.trophy.insert({
  _id: kind + '/' + user,
  user: user,
  kind: kind,
  date: new Date()
});
