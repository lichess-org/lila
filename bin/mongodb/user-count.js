print("move user nb* to user.count.*");
db.user2.find().forEach(function(user) {
  db.user2.update({
    '_id': user['_id']
  }, {
    '$unset': {
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
    '$set': {
      count: {
        ai: user.nbAi,
        draw: user.nbDraws,
        drawH: user.nbDrawsH,
        game: user.nbGames,
        loss: user.nbLosses,
        lossH: user.nbLossesH,
        rated: user.nbRatedGames,
        win: user.nbWins,
        winH: user.nbWinsH,
      }
    }
  });
});
