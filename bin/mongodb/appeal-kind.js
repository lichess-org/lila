db.appeal.find().forEach(a => {
  a.user = a._id;
  a._id = Math.random().toString(36).substring(2, 10);
  a.topic = 'legacy';
  db.appeal2.insertOne(a);
});
