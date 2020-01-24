db.msg_msg.remove({});
db.msg_thread.remove({});

print("Create db.m_thread_sorted");
if (true) {
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

  let posts = [];

  o.posts.forEach(ps => {
    ps.forEach(p => posts.push);
  });

  posts.sort((a,b) => new Date(b.createdAt) - new Date(a.createdAt));

  let msgs = posts.map(p => ({
    _id: p.id,
    text: p.text,
    date: p.createdAt,
    read: p.isRead
  }));

  let thread = {
    _id: first._id,
    users: o._id.sort(),
    lastMsg: lastMsg
  }

});
