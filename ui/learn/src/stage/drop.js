var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow,
  circle = util.circle;

var imgUrl = util.assetUrl + 'images/learn/drop.svg';

module.exports = {
  key: 'drop',
  title: 'pieceDrops',
  subtitle: 'reuseCapturedPieces',
  image: imgUrl,
  intro: 'dropIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      // lance
      goal: 'capturedPiecesCanBeDropped',
      fen: '9/9/8r/4n4/9/9/4L4/9/9 b -',
      nbMoves: 3,
      captures: 2,
      shapes: [arrow('e3e6')],
      success: assert.extinct('black'),
      highlightTakenPieceInPocket: [circle('h5'), arrow('h5i7', 'green')],
    },
    {
      // knight
      goal: 'takeTheEnemyPiecesAndDontLoseYours',
      fen: '9/9/9/2g1s4/9/9/9/6N2/9 b - 1',
      nbMoves: 4,
      captures: 2,
      success: assert.extinct('black'),
    },
    {
      // gold
      goal: 'takeTheEnemyPiecesAndDontLoseYours',
      fen: '9/5p3/4Gl3/9/9/5g3/9/9/9 b - 1',
      nbMoves: 4,
      captures: 3,
      success: assert.extinct('black'),
    },
    {
      // bish
      goal: 'takeTheEnemyPiecesAndDontLoseYours',
      fen: '4n4/g3g4/8s/9/4B4/9/9/9/9 b - 1',
      nbMoves: 7,
      captures: 4,
      success: assert.extinct('black'),
    },
    {
      // silver
      goal: 'takeTheEnemyPiecesAndDontLoseYours',
      fen: '9/3rn4/5S3/3lp4/9/7b1/9/9/9 b - 1',
      nbMoves: 7,
      captures: 5,
      success: assert.extinct('black'),
    },
    {
      // rook
      goal: 'takeTheEnemyPiecesAndDontLoseYours',
      fen: '9/9/2n1r1b2/9/3s1g1R1/9/9/9/9 b - 1',
      nbMoves: 8,
      captures: 5,
      success: assert.extinct('black'),
    },
    {
      // nifu 1
      goal: 'youCannotHaveTwoUnpromotedPawns',
      fen: '9/9/4s4/9/9/9/2PP1PP2/9/9 b P 1',
      nbMoves: 4,
      captures: 1,
      success: assert.extinct('black'),
    },
    {
      // nifu 2
      goal: 'youCannotHaveTwoUnpromotedPawns',
      fen: '9/4G4/1n2p2b1/9/9/1+P5P1/9/9/9 b - 1',
      nbMoves: 5,
      captures: 3,
      success: assert.extinct('black'),
    },
  ].map(function (l, i) {
    l.pointsForCapture = true;
    return util.toLevel(l, i);
  }),
  complete: 'captureComplete',
};
