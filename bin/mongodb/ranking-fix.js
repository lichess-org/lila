const users = db.user4;
const ranking = db.ranking;

const cheats = [];

ranking
  .find(
    {},
    {
      user: 1,
    },
  )
  .forEach(function (r) {
    if (
      r.user &&
      users.count({
        _id: r.user,
        engine: true,
      }) > 0
    )
      cheats.push(r._id);
  });

print('Found ' + cheats.length);
print(cheats.join(', '));

cheats.forEach(function (id) {
  ranking.remove({
    _id: id,
  });
});
