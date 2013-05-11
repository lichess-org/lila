// user.settings should not be an empty array, but an empty object
db.user2.find({settings:{'$in':[{},[]]}}).forEach(function(user) {
  db.user2.update({'_id': user['_id']}, {'$unset':{settings:true}});
});
db.user2.find({roles:{'$in':[{},[]]}}).forEach(function(user) {
  db.user2.update({'_id': user['_id']}, {'$unset':{roles:true}});
});
db.lobby_room.drop();
db.createCollection("lobby_room",{capped:true,size:50000})
