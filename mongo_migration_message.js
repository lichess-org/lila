// categories

print("Threads and messages");
var oThreads = db.message_thread;
var oMessages = db.message_message;
var nThreads = db.m_thread;
nThreads.drop();
oThreads.find().forEach(function(oThread) {
  var nThread = {
    _id: makeId(8),
  name: oThread.subject,
  createdAt: oThread.createdAt,
  creatorId: creatorId(oThread),
  invitedId: invitedId(oThread),
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
      isByCreator: objId(oMessage.sender).equals(nThread.creatorId),
      isRead: oMessage.isReadByParticipant[nThread.invitedId],
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
  return objId(oThread.createdBy);
}

function invitedId(oThread) {
  var c = creatorId(oThread);
  var ps = oThread.participants.map(objId);
  for (var pi in ps) {
    if (!ps[pi].equals(c)) return ps[pi];
  }
  throw "oups";
}

function userIds(oThread) {
  return [creatorId(oThread), invitedId(oThread)];
}

function visibleByUserIds(oThread) {
  var vs = [];
  userIds(oThread).forEach(function(p) {
    if (!oThread.isDeletedByParticipant[p.toString]) vs.push(p);
  });
  return vs;
}

function objId(obj) {
  return obj['$id'];
}

function makeId(size) {
  var text = "";
  var possible = "abcdefghijklmnopqrstuvwxyz0123456789";

  for( var i=0; i < size; i++ )
    text += possible.charAt(Math.floor(Math.random() * possible.length));

  return text;
}
