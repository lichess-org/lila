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
oGames.find().batchSize(batch).forEach(function(game) {
  delete game["positionHashes"];
  [0, 1].forEach(function(i) {
    delete game["players"][i]["previousMoveTs"];
    delete game["players"][i]["lastDrawOffer"];
    delete game["players"][i]["isOfferingDraw"];
    delete game["players"][i]["isProposingTakeback"];
  });
  if (game.winnerUserId) {
    game.winId = user(game.winnerUserId);
    delete game.winnerUserId;
  }
  if (game.next) {
    var next = game.next['$id'];
    if (oGames.count({_id: next}) == 1) {
      game.next = next;
    } else {
      delete game.next;
    }
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
  if (0 == it % batch) 
    print(it + "/" + totalNb + " " + Math.round(100*it/totalNb) + "%");
});

print("Indexes");
nGames.ensureIndex({status:1});
nGames.ensureIndex({userIds:1});
nGames.ensureIndex({winId:1});
nGames.ensureIndex({turns:1});
nGames.ensureIndex({updatedAt:-1});
nGames.ensureIndex({createdAt:-1});
nGames.ensureIndex({userIds:1, createdAt:-1});
