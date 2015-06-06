db.trophy.drop();
db.trophy.ensureIndex({user: 1});
db.trophy.insert({
  _id: 'zugMiracle',
  kind: 'zugMiracle',
  user: 'zugaddict',
  date: new Date()
});
db.trophy.insert({
  _id: 'wayOfBerserk',
  kind: 'wayOfBerserk',
  user: 'hiimgosu',
  date: new Date()
});
// db.trophy.insert({
//   _id: 'marathon1',
//   kind: 'marathonWinner',
//   user: 'hiimgosu',
//   date: new Date()
// });
