print("user.settings should not be an empty array, but an empty object");
db.user2.find({settings:{'$in':[{},[]]}}).forEach(function(user) {
  db.user2.update({'_id': user['_id']}, {'$unset':{settings:true}});
});

print("user.roles should not be an empty array, but an empty object");
db.user2.find({roles:{'$in':[{},[]]}}).forEach(function(user) {
  db.user2.update({'_id': user['_id']}, {'$unset':{roles:true}});
});

print("rename user.isChatBan -> user.troll");
db.user2.update({},{$rename:{isChatBan:'troll'}}, {multi:true});

print("user.settings.{chat,sound} should be a string");
['settings.chat', 'settings.sound'].forEach(function(name) {
  [true, false].forEach(function(value) {
    var sel = {}
    sel[name] = value;
    printjson(sel);
    db.user2.find(sel).forEach(function(user) {
      var up = {}
      up[name] = value.toString();
      printjson(up);
      db.user2.update({'_id': user['_id']}, {'$set':up});
    });
  });
});

print("Reset lobby_room");
db.lobby_room.drop();
db.createCollection("lobby_room",{capped:true,size:50000})

print("Reset lobby_entry");
db.lobby_entry.drop();
db.createCollection("lobby_entry",{capped:true,size:10000})
