var user = db.user4.findOne({ _id: 'thibault' });
var connections = [];
db.security
  .find({
    user: user._id,
    $or: [{ up: true }, { date: { $gt: new Date(Date.now() - 1000 * 3600 * 24 * 30 * 6) } }],
  })
  .map(o => `${o.ip} ${o.ua}`)
  .forEach(conn => {
    if (!connections.find(c => c === conn)) connections.push(conn);
  });

print(`\n${user.username} ${user.email}`);

print('\nConnections\n-----------');
print(connections.join('\n'));
