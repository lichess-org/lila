const dry = false;
const debug = false;
const diagColl = 'study_chapter_castling_diagnostic';

const fixUci = {
  e1c1: 'e1a1',
  e1g1: 'e1h1',
  e8c8: 'e8a8',
  e8g8: 'e8h8',
};
const castleChars = {
  e1c1: "'%",
  e1g1: "')",
  e8c8: '_]',
  e8g8: '_a',
  e1a1: "'#",
  e1h1: "'*",
  e8a8: '_[',
  e8h8: '_b',
};

const repairChapter = idOrDiag => {
  const diag = idOrDiag._id ? idOrDiag : db[diagColl].findOne({ _id: idOrDiag });
  const chapter = db.study_chapter_flat.findOne({ _id: diag._id });
  if (!chapter) {
    console.log('Cannot find chapter for diag ' + diag._id);
    return [];
  }
  if (debug) console.log('https://lichess.org/study/' + chapter.studyId + '/' + chapter._id);
  const moves = chapter.root;
  const moveList = Object.entries(moves);
  const moveIndexes = diag.root.map(d => moveList.findIndex(([k, _]) => k == d.k));
  diag.root.forEach((d, index) => {
    const move = moveList[moveIndexes[index]];
    if (!move) {
      // console.log(diag);
      console.log(chapter._id + ' Cannot find move for ' + d.k);
      return [chapter, null];
    }
    const fixedUci = fixUci[d.v.u];
    if (!fixedUci) {
      console.log(chapter._id + ' already has UCI ' + d.v.u + ' but wrong key ' + d.k.slice(-2));
    }
    const uci = fixedUci || d.v.u;
    move[1].u = uci;
    const key = castleChars[uci];
    if (debug) console.log(d.k + ' : ' + d.v.u + ' -> ' + uci);
    moveList.forEach(m => {
      if (m[0].startsWith(move[0])) {
        // replace the 2-chars key at the correct position
        const fixedSubKey = m[0].slice(0, d.k.length - 2) + key + m[0].slice(d.k.length);
        // if (debug) console.log('must also fix\n' + m[0] + ' as\n' + fixedSubKey);
        m[0] = fixedSubKey;
      }
    });
  });
  // Object.fromEntries doesn't preserve order, so we need to rebuild the object manually
  const fixedRoot = {};
  moveList.forEach(([k, v]) => {
    fixedRoot[k] = v;
  });
  return [chapter, fixedRoot];
};

const updateChapterRoot = (oldChapter, newRoot) => {
  if (dry) return;
  try {
    db.study_chapter_castling_backup.insertOne(oldChapter);
  } catch (e) {}
  db.study_chapter_flat.updateOne({ _id: oldChapter._id }, { $set: { root: newRoot } });
  db[diagColl].updateOne({ _id: oldChapter._id }, { $set: { repairedAt: new Date() } });
};

const findCorruptedChapters = selector => {
  const castle = (san, uci) => ({
    $and: [
      { $eq: ['$$move.v.s', san] },
      { $eq: ['$$move.v.u', uci] },
      // {
      //   $regexMatch: {
      //     input: '$$move.k',
      //     regex: uciChars,
      //   }
      // }]
    ],
  });

  db.study_chapter_flat.aggregate([
    {
      $match: {
        ...(selector || {}),
        // 'setup.variant': { $nin: [2, 3] }, // chess960, from position. The later contains chess960 games.
        // createdAt: { $gte: new Date('2025-09-19') }
      },
    },
    {
      $project: {
        // year: { $year: '$createdAt' },
        createdAt: 1,
        variant: '$setup.variant',
        root: {
          $filter: {
            input: { $objectToArray: '$root' },
            as: 'move',
            cond: {
              $or: [
                castle('O-O', 'e1g1'),
                castle('O-O-O', 'e1c1'),
                castle('O-O', 'e8g8'),
                castle('O-O-O', 'e8c8'),
              ],
            },
          },
        },
      },
    },
    { $match: { 'root.0': { $exists: true } } },
    // { $unwind: '$root' },
    // { $group: { _id: '$_id', variant: { $first: '$variant' }, createdAt: { $first: '$createdAt' }, moves: { $push: '$root' } } },
    selector ? { $merge: { into: diagColl } } : { $out: diagColl },
    // { $group: { _id: { variant: '$variant', year: '$year' }, nb: { $sum: 1 } } },
    // { $group: { _id: { year: '$year' }, nb: { $sum: 1 } } },
    // { $group: { _id: '$year', count: { $sum: 1 } } }
    // {
    // $match: {
    //   'root.k': '\')'
  ]);
};

const repairAll = nb => {
  nb = nb || db[diagColl].countDocuments();
  console.log('Repairing ' + nb + ' diagnostics');
  db[diagColl]
    .find({ repairedAt: { $exists: 0 } })
    .limit(nb)
    .forEach(diag => {
      const [c, r] = repairChapter(diag);
      if (c && r) updateChapterRoot(c, r);
      nb--;
      if (nb % 100 == 0) {
        console.log(nb + ' remaining');
      }
    });
};

repairAll();

// findCorruptedChapters({ 'setup.variant': 7 });
// const id = '8xvhgbZc';
// const [c, r] = repairChapter(id);
// updateChapterRoot(c, r);
// repairChapter(id);
// updateChapterMoves(repairChapter(id));
// findCorruptedChapters(id);
// updateChapterMoves(repairChapter(id));
