db.title_request
  .find({ 'history.0.status.n': 'approved', 'data.public': true }, { userId: 1, 'data.realName': 1 })
  .forEach(doc => {
    const u = db.user4.findOne({ _id: doc._id, 'profile.realName': '' });

    if (u) {
      console.log('Found user without realName, updating:', doc.userId);
    }
  });
