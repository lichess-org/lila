var username = '';
var userId = username.toLowerCase();
var user = db.user4.findOne({
  _id: userId,
});
if (!user) throw 'No such user';
var multi = {
  multi: true,
};
print('\n\n Delete user ' + user.username + ' with ' + user.count.game + ' games!\n\n');
sleep(5000);

print('Set black games as anon');
print(
  db.game5.update(
    {
      us: userId,
      'us.1': userId,
    },
    {
      $pull: {
        us: userId,
      },
      $unset: { p1: true },
    },
    multi,
  ).nModified + ' done',
);

print('Set white games as anon');
print(
  db.game5.update(
    {
      us: userId,
      'us.0': userId,
    },
    {
      $set: {
        'us.0': '',
      },
      $unset: { p0: true },
    },
    multi,
  ).nModified + ' done',
);
print('done');

print('Delete old PMs');
print(
  db.m_thread.deleteMany({
    visibleByUserIds: userId,
  }).deletedCount + ' done',
);

print('Delete new PMs');
print(
  db.msg_thread.deleteMany({
    users: userId,
  }).deletedCount + ' done',
);

print('Delete mod log');
print(
  db.modlog.deleteMany({
    user: userId,
  }).deletedCount + ' done',
);

print('Delete rating history');
print(
  db.history3.deleteOne({
    _id: userId,
  }).deletedCount + ' done',
);

print('Delete bookmarks');
print(
  db.bookmark.deleteMany({
    u: userId,
  }).deletedCount + ' done',
);

print('Delete learn progress');
print(
  db.learn_progress.deleteOne({
    _id: userId,
  }).deletedCount + ' done',
);

print('Delete perf stats');
var nb = 0;
for (var i = 5; i <= 20; i++)
  nb += db.perf_stat.deleteOne({
    _id: userId + '/' + i,
  }).deletedCount;
print(nb + ' done');

print('Delete prefs');
print(
  db.pref.deleteOne({
    _id: userId,
  }).deletedCount + ' done',
);

print('Delete relations from');
print(
  db.relation.deleteMany({
    u1: userId,
  }).deletedCount + ' done',
);

print('Delete relations to');
print(
  db.relation.deleteMany({
    u2: userId,
  }).deletedCount + ' done',
);

print('Delete security data');
print(
  db.security.deleteMany({
    user: userId,
  }).deletedCount + ' done',
);

print('Delete team membership');
print(
  db.team_member.deleteMany({
    user: userId,
  }).deletedCount + ' done',
);

print('Delete playbans');
print(
  db.playban.deleteOne({
    _id: userId,
  }).deletedCount + ' done',
);

print('Delete perf stats');
print(
  db.perf_stat.deleteMany({
    _id: new RegExp('^' + userId + '/'),
  }).deletedCount + ' done',
);

print('Delete activity');
print(
  db.activity.deleteMany({
    _id: new RegExp('^' + userId + ':'),
  }).deletedCount + ' done',
);

print('Delete assessments');
print(
  db.player_assessment.deleteMany({
    userId: userId,
  }).deletedCount + ' done',
);

print('Delete user');
print(
  db.user4.deleteOne({
    _id: userId,
  }).deletedCount + ' done',
  db.user_perf.deleteOne({
    _id: userId,
  }).deletedCount + ' done',
);

print('\n complete.');
