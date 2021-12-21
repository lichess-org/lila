var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/bowman.svg';

module.exports = {
  key: 'capture',
  title: 'capture',
  subtitle: 'takeTheEnemyPieces',
  image: imgUrl,
  intro: 'captureIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      // lance
      goal: 'takeTheEnemyPieces',
      fen: '9/4n4/9/9/4p4/9/9/4L4/9 b -',
      nbMoves: 2,
      captures: 2,
      shapes: [arrow('e2e5'), arrow('e5e8')],
      success: assert.extinct('white'),
    },
    {
      // gold
      goal: 'takeTheEnemyPiecesAndDontLoseYours',
      fen: '9/9/4nr3/4G4/9/9/9/9/9 b -',
      nbMoves: 2,
      captures: 2,
      success: assert.extinct('white'),
    },
    {
      // bishop
      goal: 'takeTheEnemyPiecesAndDontLoseYours',
      fen: '9/9/9/4p4/9/4B1s2/5g3/9/9 b -',
      nbMoves: 4,
      captures: 3,
      success: assert.extinct('white'),
    },
    {
      // knight
      goal: 'takeTheEnemyPiecesAndDontLoseYours',
      fen: '9/3spg3/3p1p3/4N4/9/9/9/9/9 b -',
      nbMoves: 5,
      captures: 5,
      success: assert.extinct('white'),
    },
    {
      // rook
      goal: 'takeTheEnemyPiecesAndDontLoseYours',
      fen: '9/4r1s2/5n3/3R1b3/9/9/9/9/9 b -',
      nbMoves: 5,
      captures: 4,
      success: assert.extinct('white'),
    },
    {
      // silver
      goal: 'takeTheEnemyPiecesAndDontLoseYours',
      fen: '9/5l+p2/4S4/4pp3/9/9/9/9/9 b -',
      nbMoves: 7,
      captures: 4,
      success: assert.extinct('white'),
    },
  ].map(function (l, i) {
    l.pointsForCapture = true;
    l.noPocket = true;
    return util.toLevel(l, i);
  }),
  complete: 'captureComplete',
};
