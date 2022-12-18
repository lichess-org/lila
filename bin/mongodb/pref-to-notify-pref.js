db.pref.find({ $or: [{ mention: false }, { corresEmailNotif: true }] }).forEach(p => {
  db.notify_pref.insertOne({
    _id: p._id,
    privateMessage: 7,
    challenge: 7,
    mention: p.mention ? 7 : 0,
    streamStart: 7,
    tournamentSoon: 6,
    gameEvent: 6,
    invitedStudy: 7,
    correspondenceEmail: p.corresEmailNotif,
  });
});
