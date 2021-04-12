// CONFIGURE ME!
mainDb = Mongo().getDB('lichess');
puzzleDb = Mongo().getDB('puzzler');
studyDb = Mongo().getDB('lichess');
// CONFIG END

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

const scrub = (collName, inDb) => f => {
  print(`- ${collName}`);
  // sleep(500);
  return f((inDb || mainDb)[collName]);
};

const userId = user._id;
const ghostId = `!${randomId()}`;
const tos =
  user.marks && (user.marks.engine || user.marks.boost || user.marks.troll || user.marks.rankban || user.marks.alt);

scrub('clas_clas')(c => {
  c.updateMany({ 'created.by': userId }, { $set: { 'created.by': ghostId } });
  c.updateMany({ teachers: userId }, { $pull: { teachers: userId } });
});

scrub('clas_student')(c => c.remove({ userId: userId }));

scrub('crosstable2')(c => c.remove({ _id: new RegExp(`^${userId}/`) }));

scrub('eval_cache')(c => c.updateMany({ 'evals.by': userId }, { $set: { 'evals.$.by': ghostId } }));

scrub('f_post')(c => c.updateMany({ userId: userId }, { $set: { userId: ghostId } }));

scrub('game5')(c => {
  c.updateMany({ wid: userId }, { $set: { wid: ghostId } });
  c.updateMany({ us: userId }, { $set: { 'us.$': ghostId } });
});

scrub('puzzle2_puzzle', puzzleDb)(c => c.updateMany({ users: userId }, { $set: { 'users.$': ghostId } }));

scrub('puzzle2_round', puzzleDb)(c => c.remove({ _id: new RegExp(`^${userId}:`) }));

scrub('report2')(c => {
  c.updateMany({ 'atoms.by': userId }, { $set: { 'atoms.$.by': ghostId } });
  !tos && c.updateMany({ user: userId }, { $set: { user: ghostId } });
});

const studyIds = scrub(
  'study',
  studyDb
)(c => {
  c.updateMany({ likers: userId }, { $pull: { likers: userId } });
  const ids = c.distinct('_id', { uids: userId });
  c.updateMany({ uids: userId }, { $pull: { uids: userId }, $unset: { [`members.${userId}`]: true } });
  c.updateMany({ ownerId: userId }, { $set: { ownerId: ghostId } });
  return ids;
});

scrub(
  'study_chapter_flat',
  studyDb
)(c => {
  c.updateMany({ _id: { $in: studyIds }, ownerId: userId }, { $set: { ownerId: ghostId } });
});

scrub('simul')(c => {
  c.updateMany({ hostId: userId }, { $set: { hostId: ghostId } });
  c.updateMany({ 'pairings.player.user': userId }, { $set: { 'pairings.$.player.user': ghostId } });
});

const swissIds = scrub('swiss')(c => {
  c.updateMany({ winnerId: userId }, { $set: { winnerId: ghostId } });
  return db.swiss_player.distinct('s', { u: userId });
});

if (swissIds.length) {
  scrub('swiss_player')(c => {
    c.find({ _id: { $in: swissIds.map(s => `${s}:${userId}`) } }).forEach(p => {
      c.remove({ _id: p._id });
      p._id = `${p.s}:${ghostId}`;
      p.u = ghostId;
      c.insert(p);
    });
  });
  scrub('swiss_pairing')(c => c.updateMany({ s: { $in: swissIds }, p: userId }, { $set: { 'p.$': ghostId } }));
}

scrub('team')(c => {
  c.updateMany({ createdBy: userId }, { $set: { createdBy: ghostId } });
  c.updateMany({ leaders: userId }, { $pull: { leaders: userId } });
});

scrub('team_request')(c => c.remove({ user: userId }));

scrub('team_member')(c => c.remove({ user: userId }));

scrub('timeline_entry')(c => c.remove({ users: userId }));

scrub('tournament2')(c => {
  c.updateMany({ winnerId: userId }, { $set: { winnerId: ghostId } });
  c.updateMany({ createdBy: userId }, { $set: { createdBy: ghostId } });
});
const arenaIds = scrub('tournament_leaderboard')(c => {
  const ids = c.distinct('t', { u: userId });
  c.remove({ u: userId });
  return ids;
});
if (arenaIds.length) {
  scrub('tournament_player')(c => c.updateMany({ tid: { $in: arenaIds }, uid: userId }, { $set: { uid: ghostId } }));
  scrub('tournament_pairing')(c => c.updateMany({ tid: { $in: arenaIds }, u: userId }, { $set: { 'u.$': ghostId } }));
}

/*
We decided not to delete PMs out of legit interest of the correspondents
and also to be able to comply to data requests from law enforcement

const msgThreadIds = scrub('msg_thread')(c => {
  const ids = c.distinct('_id', { users: userId });
  c.remove({ users: userId });
  return ids;
});
scrub('msg_msg')(c => msgThreadIds.length && c.remove({ tid: { $in: msgThreadIds } }));
*/
