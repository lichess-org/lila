print("Hashing users")
var userHash = {};
db.user2.find({},{oid:1}).forEach(function(user) {
  userHash[user.oid.toString()] = user._id;
});
function user(oid) {
  if(userHash[oid]) return userHash[oid];
  throw "Missing user " + oid;
}

print("Games");
var batch = 10000;
var oGames = db.game;
var nGames = db.game2;
var it = 0, totalNb = oGames.count();
nGames.drop();
oGames.find().batchSize(batch).limit(100000).forEach(function(game) {
  delete game["positionHashes"];
  delete game["players.0.previousMoveTs"];
  delete game["players.1.previousMoveTs"];
  delete game["players.0.lastDrawOffer"];
  delete game["players.1.lastDrawOffer"];
  delete game["players.0.isOfferingDraw"];
  delete game["players.1.isOfferingDraw"];
  delete game["players.0.isProposingTakeback"];
  delete game["players.1.isProposingTakeback"];
  if (game.winnerUserId) {
    game.winId = user(game.winnerUserId);
    delete game.winnerUserId;
  }
  if (game.userIds) {
    var userIds = [];
    game.userIds.forEach(function(oid) { userIds.push(user(oid)); });
    game.userIds = userIds;
    [0, 1].forEach(function(i) {
      if (game["players"][i]["user"]) {
        game["players"][i]["uid"] = user(game["players"][i]["user"]['$id'].toString());
        delete game["players"][i]["user"];
      }
      if (game["players"][i]["eloDiff"]) {
        game["players"][i]["ed"] = game["players"][i]["eloDiff"];
        delete game["players"][i]["eloDiff"];
      }
    });
  }
  nGames.insert(game);
  ++it;
  if (0 == it % batch) print(it + "/" + totalNb);
});
