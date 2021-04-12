if (typeof user == 'undefined') throw 'Usage: mongo lichess --eval \'user="username"\' script.js';

user = db.user4.findOne({ _id: user });

// if (!user || user.enabled || !user.erasedAt) throw 'Erase with lichess CLI first.';

// print(`\n\n Delete user ${user.username}!\n\n`);
// sleep(5000);

const randomId = () => {
  const idChars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const idLength = 8;
  let result = '';
  for (let i = idLength; i > 0; --i) result += idChars[Math.floor(Math.random() * idChars.length)];
  return result;
};

const scrub = collName => f => {
  print(`- ${collName}`);
  // sleep(500);
  f(db[collName]);
};

const userId = user._id;
const ghostId = `!${randomId()}`;

// db.getMongo().setReadPref('secondary');

scrub('clas_clas')(c => {
  c.updateMany({ 'created.by': userId }, { $set: { 'created.by': ghostId } });
  c.updateMany({ teachers: userId }, { $set: { 'teachers.$': ghostId } });
});

scrub('clas_student')(c => c.remove({ userId: userId }));

scrub('crosstable2')(c => c.remove({ _id: new RegExp(`^${userId}/`) }));

scrub('eval_cache')(c => c.updateMany({ 'evals.by': userId }, { $set: { 'evals.$.by': ghostId } }));

scrub('f_post')(c => c.updateMany({ userId: userId }, { $set: { userId: ghostId } }));

scrub('game5')(c => {
  c.updateMany({ wid: userId }, { $set: { wid: ghostId } });
  c.updateMany({ us: userId }, { $set: { 'us.$': ghostId } });
});
