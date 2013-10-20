var usersToMigrate = db.user2.find({settings:{$exists:1}},{settings:1});
var collection = db.pref;

print("Migrating " + usersToMigrate.count() + " prefs");

collection.drop();

usersToMigrate.forEach(function(u) {
  db.user2.update({_id:u._id},{$unset:{'settings':true}});
  collection.insert({
    _id: u._id,
    dark: u.settings.bg == 'dark',
    theme: u.settings.theme
  });
});

print("Done!");
