const daysBack = 7;

const now = Date.now();

rs.slaveOk();

function daysAgo(days) {
  return new Date(now - days * 24 * 60 * 60 * 1000);
}

for (let i = daysBack; i >= 0; i--) {
  const from = daysAgo(i + 1),
    to = daysAgo(i);
  const games = db.game5.count({ ca: { $gte: from, $lt: to } });
  print(i + ' days ago, from ' + from + ' to ' + to + ' -> ' + games + ' games');
}
