print('move user nb* to user.count.*');
db.user2.find().forEach(function (user) {
  db.user2.update(
    {
      _id: user['_id'],
    },
    {
      $unset: {
        nbAi: true,
        nbDraws: true,
        nbDrawsH: true,
        nbGames: true,
        nbLosses: true,
        nbLossesH: true,
        nbRatedGames: true,
        nbWins: true,
        nbWinsH: true,
      },
      $set: {
        count: {
          ai: user.nbAi || 0,
          draw: user.nbDraws || 0,
          drawH: user.nbDrawsH || 0,
          game: user.nbGames || 0,
          loss: user.nbLosses || 0,
          lossH: user.nbLossesH || 0,
          rated: user.nbRatedGames || 0,
          win: user.nbWins || 0,
          winH: user.nbWinsH || 0,
        },
      },
    },
  );
});
