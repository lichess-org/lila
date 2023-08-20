var studyIds =
  '8yadFPpU BJy6fEDf Rg2cMBZ6 fE4k21MW 8yadFPpU PDkQDt6u 96Lij7wH 9ogFv8Ac xebrDvFe A4ujYOer'.split(' ');

// from 4r1qk/5p1p/pp2rPpR/2pbP1Q1/3pR3/2P5/P5PP/2B3K1 w - - 2 28
// to   4r1qk/5p1p/pp2rPpR/2pbP1Q1/3pR3/2P5/P5PP/2B3K1 w - - 0 1
function fixFen(fen) {
  var parts = fen.split(' ');
  parts[4] = '0';
  parts[5] = '1';
  return parts.join(' ');
}

function makePly(fen) {
  return fen.split(' ')[1] === 'w' ? 0 : 1;
}

var chapters = db.study_chapter
  .find({
    studyId: {
      $in: studyIds,
    },
  })
  .forEach(function (chapter) {
    var fen = chapter.root.f;
    var fixed = fixFen(fen);
    if (fixed != fen) {
      print('Fix chapter FEN ' + chapter._id + ': ' + fixed);
      db.study_chapter.update(
        {
          _id: chapter._id,
        },
        {
          $set: {
            'root.f': fixed,
          },
        },
      );
    }

    var ply = makePly(fixed);
    if (chapter.root.p != ply) {
      print('Fix chapter root ply ' + chapter._id);
      db.study_chapter.update(
        {
          _id: chapter._id,
        },
        {
          $set: {
            'root.p': NumberInt(ply),
          },
        },
      );
    }
  });
