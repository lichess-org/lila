db.msg_msg.remove({});
db.msg_thread.remove({});

if (false) {
  print("Create db.m_thread_sorted");
  db.m_thread_sorted.drop();
  db.m_thread.find({visibleByUserIds:{$size:2}}).forEach(t => {
    t.visibleByUserIds.sort();
    db.m_thread_sorted.insert(t);
  });
}

print("Create db.msg_thread");
db.m_thread_sorted.aggregate([
  {$group:{_id:'$visibleByUserIds',threads:{$push:'$$ROOT'}}}
]).forEach(o => {

  let first = o.threads[0];

  let msgs = [];

  o.threads.forEach(t => {
    t.posts.forEach(p => {
      msgs.push({
        _id: p.id,
        thread: first._id,
        text: p.text,
        user: p.isByCreator ? t.creatorId : t.invitedId,
        date: p.createdAt
      });
    });
  });

  msgs.sort((a,b) => new Date(b.date) - new Date(a.date));

  let last = msgs[msgs.length - 1];

  let thread = {
    _id: first._id,
    users: o._id.sort(),
    lastMsg: {
      text: last.text.slice(0, 60),
      user: last.user,
      date: last.date,
      read: !o.threads.find(t => t.posts.find(p => p.isRead))
    }
  }

  db.msg_thread.insert(thread);
  db.msg_msg.insertMany(msgs, {ordered: false});
});
