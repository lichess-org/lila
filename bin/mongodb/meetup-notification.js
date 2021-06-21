var sender = 'lichess';
var threadName = 'Lichess meetup in London, November 24th';

var sent = 0;

db.user4
  .find({
    'profile.location': 'montreal',
    enabled: true,
    seenAt: { $gt: new Date(Date.now() - 1000 * 3600 * 24 * 30) },
  })
  .forEach(user => {
    if (
      db.notify.count({
        notifies: user._id,
        'content.type': 'privateMessage',
        'content.thread.name': threadName,
      })
    )
      return;
    var threadId = Math.random().toString(36).substring(2, 10);
    db.notify.insert({
      _id: Math.random().toString(36).substring(2, 10),
      notifies: user._id,
      content: {
        type: 'privateMessage',
        senderId: sender,
        thread: {
          id: threadId,
          name: threadName,
        },
        text: threadName,
      },
      read: false,
      createdAt: new Date(),
    });
    print(user.username);
    db.m_thread.insert({
      _id: threadId,
      name: threadName,
      createdAt: new Date(),
      updatedAt: new Date(),
      posts: [
        {
          id: Math.random().toString(36).substring(2, 10),
          text: 'You are most welcome to join us!\n\nhttps://lichess.org/blog/W-nQzxYAAC8AaUUo/lichess-london-meetup-on-the-24th-november\n\nYou received this message because you connected from the UK, Ireland, France or Belgium.',
          isByCreator: true,
          isRead: false,
          createdAt: new Date(),
        },
      ],
      creatorId: sender,
      invitedId: user._id,
      visibleByUserIds: ['lichess', user._id],
      mod: true,
    });
    sent++;
  });

print('Sent ' + sent);
