db.pref.find({ $or: [{ mention: false }, { corresEmailNotif: true }] }).forEach(p => {
  db.notify_pref.insertOne({
    _id: p._id,
    privateMessage: NumberInt(7),
    challenge: NumberInt(7),
    mention: NumberInt(p.mention ? 7 : 0),
    streamStart: NumberInt(7),
    tournamentSoon: NumberInt(6),
    gameEvent: NumberInt(6),
    invitedStudy: NumberInt(7),
    correspondenceEmail: p.corresEmailNotif,
  });
});
