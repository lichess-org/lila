db.fos_user_group.drop();
db.user.dropIndex("emailCanonical_1");
db.user.dropIndex("isOnline_-1");
db.user.update({},{$unset:{
  lastLogin: true,
  isOnline: true,
  gameConfigs: true,
  email: true,
  emailCanonical: true,
  updatedAt: true,
  algorithm: true,
  rememberMeToken: true
}}, false, true);
db.user.update({elo: {$lt: 800}}, {$set: {elo: 800}}, false, true)
