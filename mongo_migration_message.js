print("Hashing users")
var users = {};
db.user2.find({},{oid:1}).forEach(function(user) {
  users[user.oid.toString()] = user._id;
});

print("Threads and messages");
var oThreads = db.message_thread;
var oMessages = db.message_message;
var nThreads = db.m_thread;
nThreads.drop();
oThreads.find().forEach(function(oThread) {
  var invOid = invitedOid(oThread);
  var nThread = {
    _id: makeId(8),
  name: oThread.subject,
  createdAt: oThread.createdAt,
  creatorId: creatorId(oThread),
  invitedId: users[invOid],
  visibleByUserIds: visibleByUserIds(oThread)
  };
  var posts = [];
  oMessages
  .find({"thread.$id": oThread._id})
  .sort({createdAt: 1})
  .forEach(function(oMessage) {
    posts.push({
      id: makeId(8),
      text: oMessage.body.replace(/\\r\\n/, "\n"),
      isByCreator: username(oMessage.sender) == nThread.creatorId,
      isRead: oMessage.isReadByParticipant[invOid],
      createdAt: oMessage.createdAt
    });
  });
if(posts.length > 0) {
  nThread.posts = posts;
  nThread.updatedAt = posts[posts.length -1].createdAt;
  nThreads.insert(nThread)
} else {
  print("Skip empty thread " + nThread.name);
}
});
nThreads.ensureIndex({visibleByUserIds: 1});
nThreads.ensureIndex({visibleByUserIds: 1, updatedAt: -1});
print("Done threads and messages");

function creatorId(oThread) {
  return username(oThread.createdBy);
}

function invitedOid(oThread) {
  for (var pi in oThread.participants) {
    if (objId(oThread.participants[pi]) != objId(oThread.createdBy))
      return objId(oThread.participants[pi]);
  }
  throw "oups";
}

function userIds(oThread) {
  return [creatorId(oThread), users[invitedOid(oThread)]];
}

function visibleByUserIds(oThread) {
  var vs = [];
  userIds(oThread).forEach(function(p) {
    if (!oThread.isDeletedByParticipant[p.toString]) vs.push(p);
  });
  return vs;
}

function username(obj) {
  if (typeof obj == "object") return users[objId(obj)];
  return users[obj];
}

function objId(obj) {
  return obj['$id'].toString();
}

function makeId(size) {
  var text = "";
  var possible = "abcdefghijklmnopqrstuvwxyz0123456789";

  for( var i=0; i < size; i++ )
    text += possible.charAt(Math.floor(Math.random() * possible.length));

  return text;
}
