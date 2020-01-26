db.msg_msg.remove({});
db.msg_thread.remove({});

const now = Date.now();

print("Delete old notifications");
db.notify.remove({'content.type':'privateMessage'});

if (false || !db.m_thread_sorted.count()) {
  print("Create db.m_thread_sorted");
  db.m_thread_sorted.drop();
  db.m_thread.find({visibleByUserIds:{$size:2}}).forEach(t => {
    if (t.creatorId == t.invitedId) return;
    t.visibleByUserIds.sort();
    db.m_thread_sorted.insert(t);
  });
}

print("Create db.msg_thread");
db.m_thread_sorted.aggregate([
  {$group:{_id:'$visibleByUserIds',threads:{$push:'$$ROOT'}}}
],{ allowDiskUse: true }).forEach(o => {

  let userIds = o.threads[0].visibleByUserIds;
  userIds.sort();
  let threadId = userIds.join('/');

  let msgs = [];

  o.threads.forEach(t => {
    t.posts.forEach(p => {
      msgs.push({
        _id: p.id,
        thread: threadId,
        text: p.text,
        user: p.isByCreator ? t.creatorId : t.invitedId,
        date: p.createdAt
      });
    });
  });

  msgs.sort((a,b) => new Date(a.date) - new Date(b.date));

  let last = msgs[msgs.length - 1];

  let thread = {
    _id: threadId,
    users: userIds,
    lastMsg: {
      text: last.text.slice(0, 60),
      user: last.user,
      date: last.date,
      read: !o.threads.find(t => t.posts.find(p => p.isRead)) || isOld(last.date)
    }
  }

  db.msg_thread.insert(thread);
  db.msg_msg.insertMany(msgs, {ordered: false});
});

function isOld(date) {
  return now - date > 1000 * 3600 * 24 * 7;
}
