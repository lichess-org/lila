db.user2.find({}).forEach(function(user) {
  var uid = user['_id'];
  var data = {
    nbWins: db.game2.count({"winId": uid, "players.aiLevel":{$exists:false}}),
    nbLosses: db.game2.count({"userIds": uid, "status": { "$in": [ 30, 31, 35, 33 ] }, "winId": {"$ne": uid}, "players.aiLevel":{$exists:false}}),
    nbDraws: db.game2.count({"userIds": uid, "status": { "$in": [34, 32] }, "players.aiLevel":{$exists:false}})
  };
  db.user2.update({"_id": uid}, {"$set":data});
});
