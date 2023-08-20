db.msg_msg.drop();
db.msg_thread.drop();

function makeIndexes() {
  db.msg_thread.ensureIndex({ users: 1, 'lastMsg.date': -1 }, { background: 1 });
  db.msg_thread.ensureIndex(
    { users: 1 },
    { partialFilterExpression: { 'lastMsg.read': false }, background: 1 },
  );
  db.msg_msg.ensureIndex({ tid: 1, date: -1 }, { background: 1 });
}

const now = Date.now();

print('Delete old notifications');
db.notify.remove({ 'content.type': 'privateMessage' });

if (!db.m_thread_sorted.count()) {
  print('Create db.m_thread_sorted');
  db.m_thread_sorted.drop();
  db.m_thread
    .find({
      mod: { $exists: false },
      visibleByUserIds: { $size: 2 },
      $or: [
        {
          creatorId: {
            $nin: [
              'lichess',
              'lichess-qa',
              'lichess-blog',
              'lichess-team',
              'mirlife',
              'lichess4545',
              'whatnext',
            ],
          },
        },
        {
          updatedAt: { $gt: new Date(Date.now() - 1000 * 3600 * 24 * 14) },
        },
      ],
    })
    .forEach(t => {
      if (t.creatorId == t.invitedId) return;
      t.visibleByUserIds.sort();
      db.m_thread_sorted.insert(t);
    });
}

print('Create db.msg_thread');
db.m_thread_sorted
  .aggregate([{ $group: { _id: '$visibleByUserIds', threads: { $push: '$$ROOT' } } }], { allowDiskUse: true })
  .forEach(o => {
    let userIds = o.threads[0].visibleByUserIds;
    userIds.sort();
    let threadId = userIds.join('/');

    let msgs = [];

    o.threads.forEach(t => {
      t.posts.forEach(p => {
        if (o.creatorId == 'lichess' && isOld(p.createdAt)) return;
        msgs.push({
          _id: p.id,
          tid: threadId,
          text: p.text,
          user: p.isByCreator ? t.creatorId : t.invitedId,
          date: p.createdAt,
        });
      });
    });

    if (!msgs.length) return;

    msgs.sort((a, b) => new Date(a.date) - new Date(b.date));

    let last = msgs[msgs.length - 1];

    let thread = {
      _id: threadId,
      users: userIds,
      lastMsg: {
        text: last.text.slice(0, 60),
        user: last.user,
        date: last.date,
        read: !o.threads.find(t => t.posts.find(p => p.isRead)) || isOld(last.date),
      },
    };

    db.msg_msg.insertMany(msgs, { ordered: false });
    try {
      db.msg_thread.insert(thread);
    } catch (e) {}
  });

makeIndexes();

function isOld(date) {
  return now - date > 1000 * 3600 * 24 * 7;
}
