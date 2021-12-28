db.tournament_player.remove({ tid: 'qzRBGPLN', r: { $lt: 2000 } });
db.tournament2.update(
  { _id: 'qzRBGPLN' },
  {
    $set: {
      nbPlayers: db.tournament_player.count({ tid: 'qzRBGPLN' }),
      minutes: NumberInt(80),
      status: NumberInt(10),
      startsAt: ISODate('2021-12-22T19:07:40.000Z'),
    },
    $unset: { winner: 1 },
  }
);
