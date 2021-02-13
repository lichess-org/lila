var usersToMigrate = db.user2.find();
var collection = db.user3;

print('Migrating ' + usersToMigrate.count() + ' users');

collection.drop();

function nn(x) {
  return (x | '') !== '';
}

usersToMigrate.forEach(function (u) {
  if ((u.bio | '') !== '') u.profile = { bio: u.bio };
  delete u.bio;

  collection.insert(u);
});

print('Done!');
