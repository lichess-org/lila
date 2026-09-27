let nb = 0;
db.title_request
  .find({ 'history.0.status.n': 'approved', 'data.public': true }, { userId: 1, 'data.realName': 1 })
  .forEach(doc => {
    const u = db.user4.findOne({ _id: doc.userId, 'profile.realName': { $exists: 0 } });

    if (u) {
      console.log('Found user without realName, updating:', doc.userId);
      db.user4.updateOne({ _id: doc.userId }, { $set: { 'profile.realName': doc.data.realName } });
      nb++;
    }
  });

console.log('Total users without realName updated:', nb);
