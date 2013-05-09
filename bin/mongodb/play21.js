// user.settings should not be an empty array, but an empty object
db.user2.find({settings:[]}).forEach(function(user) {
  db.user2.update({'_id': user['_id']}, {'$set':{settings:{}}});
});
db.lobby_room.drop();
db.createCollection("lobby_room",{capped:true,size:50000})
