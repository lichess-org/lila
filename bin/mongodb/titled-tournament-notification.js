var tournamentId = 'qkdW41M2';
var text = 'With a prize pool of $3200!';

var userIds = db.user4.distinct('_id', {
  enabled: true,
  title: {
    $exists: true,
    $nin: ['LM', 'BOT'],
  },
});
'thibault arex'.split(' ').forEach(u => userIds.push(u));

print('Inviting ' + userIds.join(', '));

var invited = 0;

userIds.forEach(userId => {
  if (
    db.notify.count({
      notifies: userId,
      'content.type': 'titledTourney',
      'content.id': tournamentId,
    })
  )
    return;
  db.notify.insert({
    _id: Math.random().toString(36).substring(2, 10),
    notifies: userId,
    content: {
      type: 'titledTourney',
      id: tournamentId,
      text: text,
    },
    read: false,
    createdAt: new Date(),
  });
  invited++;
});

print('Invited ' + invited + ' out of ' + userIds.length + ' titled players');
