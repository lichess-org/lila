db.tournament2
  .find({ 'schedule.freq': 'shield', nbPlayers: 0, startsAt: { $gt: new Date() } })
  .forEach(function (tournament) {
    console.log('Deleting ', tournament._id, ' ', tournament.name);
    db.tournament_player.deleteMany({ tid: tournament._id });
    db.tournament2.deleteOne({ _id: tournament._id });
  });
