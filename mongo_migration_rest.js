print("Other collections");

db.lobby_room.drop();
db.createCollection("lobby_room", {capped:true, max:1000})

db.lobby_entry.drop();
db.createCollection("lobby_entry", {capped:true, max:50})

db.hook.drop();
db.hook.ensureIndex({ownerId: 1});
db.hook.ensureIndex({mode: 1});
db.hook.ensureIndex({createdAt: -1});
db.hook.ensureIndex({match: 1});
