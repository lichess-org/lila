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

const newGhostId = () => {
  const idChars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const idLength = 8;
  let id = '';
  for (let i = idLength; i > 0; --i) id += idChars[Math.floor(Math.random() * idChars.length)];
  return `!${id}`;
};

const scrub = (collName, inDb) => f => {
  print(`- ${collName}`);
  sleep(200);
  return f((inDb || mainDb)[collName]);
};

const byId = doc => ({ _id: doc._id });
const deleteAll = (collName, field, value) => scrub(collName)(c => c.remove({ [field]: value || userId }));
const setNewGhostId = (coll, doc, field) => coll.update(byId(doc), { $set: { [field]: randomId() } });
const replaceWithNewGhostIds = (collName, field, inDb) =>
  scrub(collName, inDb)(c => c.find({ [field]: userId }, { _id: 1 }).forEach(doc => setNewGhostId(c, doc, field)));

const userId = user._id;
const tos =
  user.marks && (user.marks.engine || user.marks.boost || user.marks.troll || user.marks.rankban || user.marks.alt);

// Let us scrub.

deleteAll('activity', '_id', new RegExp(`^${userId}:`));

deleteAll('analysis_requester', '_id');

deleteAll('bookmark', 'u');

deleteAll('challenge', 'challenger.id');
deleteAll('challenge', 'destUser.id');

replaceWithNewGhostIds('clas_clas', 'created.by');
scrub('clas_clas')(c => c.updateMany({ teachers: userId }, { $pull: { teachers: userId } }));

deleteAll('clas_student', 'userId');

deleteAll('coach', '_id');

deleteAll('coach_review', 'userId');

deleteAll('config', '_id');

deleteAll('coordinate_score', '_id');

deleteAll('crosstable2', '_id', new RegExp(`^${userId}/`));

replaceWithNewGhostIds('f_post', 'userId');

scrub('game5')(c =>
  c.find({ us: userId }, { us: 1, wid: 1 }).forEach(doc => {
    const gameGhostId = newGhostId();
    c.update(byId(doc), {
      $set: {
        [`us.${doc.us.indexOf(userId)}`]: gameGhostId, // replace player usernames
        ...(doc.wid == userId ? { wid: gameGhostId } : {}), // replace winner username
      },
    });
  })
);

deleteAll('history3', '_id');

deleteAll('image', 'createdBy');

scrub(
  'puzzle2_puzzle',
  puzzleDb
)(c =>
  c.find({ userId: userId }, { users: 1 }).forEach(doc =>
    c.update(byId(doc), {
      $set: {
        [`users.${doc.users.indexOf(userId)}`]: newGhostId(),
      },
    })
  )
);

deleteAll('puzzle2_round', '_id', new RegExp(`^${userId}:`));

scrub('report2')(c => {
  c.find({ 'atoms.by': userId }, { atoms: 1 }).forEach(doc => {
    const reportGhostId = newGhostId();
    const newAtoms = doc.atoms.map(a => ({
      ...a,
      by: a.by == userId ? reportGhostId : a.by,
    }));
    c.update(byId(doc), { $set: { atoms: newAtoms } });
  });
  !tos && c.updateMany({ user: userId }, { $set: { user: newGhostId() } });
});

replaceWithNewGhostIds('study', 'ownerId', studyDb);
const studyIds = scrub(
  'study',
  studyDb
)(c => {
  c.updateMany({ likers: userId }, { $pull: { likers: userId } });
  const ids = c.distinct('_id', { uids: userId });
  c.updateMany({ uids: userId }, { $pull: { uids: userId }, $unset: { [`members.${userId}`]: true } });
  return ids;
});

scrub(
  'study_chapter_flat',
  studyDb
)(c =>
  c.find({ _id: { $in: studyIds }, ownerId: userId }, { _id: 1 }).forEach(doc => setNewGhostId(c, doc, 'ownerId'))
);

replaceWithNewGhostIds('simul', 'hostId');
scrub('simul')(c =>
  c.find({ 'pairings.player.user': userId }, { pairings: 1 }).forEach(doc => {
    doc.pairings.forEach(p => {
      if (p.player.user == userId) p.player.user = newGhostId();
    });
    c.update(byId(doc), { $set: { pairings: doc.pairings } });
  })
);

replaceWithNewGhostIds('swiss', 'winnerId');

const swissIds = scrub('swiss_player')(c => c.distinct('s', { u: userId }));

if (swissIds.length) {
  // here we use a single ghost ID for all swiss players and pairings,
  // because the mapping of swiss player to swiss pairings must be preserved
  const swissGhostId = newGhostId();
  scrub('swiss_player')(c => {
    c.find({ _id: { $in: swissIds.map(s => `${s}:${userId}`) } }).forEach(p => {
      c.remove({ _id: p._id });
      p._id = `${p.s}:${swissGhostId}`;
      p.u = swissGhostId;
      c.insert(p);
    });
  });
  scrub('swiss_pairing')(c => c.updateMany({ s: { $in: swissIds }, p: userId }, { $set: { 'p.$': swissGhostId } }));
}

replaceWithNewGhostIds('team', 'createdBy');
scrub('team')(c => c.updateMany({ leaders: userId }, { $pull: { leaders: userId } }));

deleteAll('team_request', 'user');

deleteAll('team_member', 'user');

replaceWithNewGhostIds('tournament2', 'createdBy');
replaceWithNewGhostIds('tournament2', 'winnerId');

const arenaIds = scrub('tournament_leaderboard')(c => c.distinct('t', { u: userId }));
if (arenaIds.length) {
  // here we use a single ghost ID for all arena players and pairings,
  // because the mapping of arena player to arena pairings must be preserved
  const arenaGhostId = newGhostId();
  scrub('tournament_player')(c =>
    c.updateMany({ tid: { $in: arenaIds }, uid: userId }, { $set: { uid: arenaGhostId } })
  );
  scrub('tournament_pairing')(c =>
    c.updateMany({ tid: { $in: arenaIds }, u: userId }, { $set: { 'u.$': arenaGhostId } })
  );
  deleteAll('tournament_leaderboard', 'u');
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
