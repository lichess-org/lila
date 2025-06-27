db.trophy.drop();
db.trophy.ensureIndex({ user: 1 });
db.trophy.insert({
  _id: 'zugMiracle',
  kind: 'zugMiracle',
  user: 'zugaddict',
  date: new Date(),
});
