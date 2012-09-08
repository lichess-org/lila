db.user2.find({}).forEach(function(user) {
  var uid = user['_id'];
  var data = {
    nbWins: db.game2.count({"winId": uid}),
    nbLosses: db.game2.count({"userIds": uid, "status": { "$in": [ 30, 31, 35, 33 ] }, "winId": {"$ne": uid}}),
    nbDraws: db.game2.count({"userIds": uid, "status": { "$in": [34, 32] }}),
    nbWinsH: db.game2.count({"winId": uid, "players.aiLevel":{$exists:false}}),
    nbLossesH: db.game2.count({"userIds": uid, "status": { "$in": [ 30, 31, 35, 33 ] }, "winId": {"$ne": uid}, "players.aiLevel":{$exists:false}}),
    nbDrawsH: db.game2.count({"userIds": uid, "status": { "$in": [34, 32] }, "players.aiLevel":{$exists:false}}),
    nbAi: db.game2.count({"userIds": uid, "players.aiLevel":{$exists:true}})
  };
  db.user2.update({"_id": uid}, {"$set":data});
});
