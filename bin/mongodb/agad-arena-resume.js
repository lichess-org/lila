db.tournament_player.remove({ tid: 'qzRBGPLN', r: { $lt: 2000 } });
db.tournament_pairing.remove({ tid: 'qzRBGPLN', d: { $gte: ISODate('2021-12-21T20:02:00.000Z') } });
db.tournament2.update(
  { _id: 'qzRBGPLN' },
  {
    $set: {
      nbPlayers: db.tournament_player.count({ tid: 'qzRBGPLN' }),
      minutes: NumberInt(80),
      status: NumberInt(10),
      startsAt: ISODate('2022-01-11T19:00:00.000Z'),
    },
    $unset: { winner: 1 },
  },
);
