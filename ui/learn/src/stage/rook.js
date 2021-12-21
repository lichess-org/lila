var util = require('../util');
var arrow = util.arrow;
var circle = util.circle;

module.exports = {
  key: 'rook',
  title: 'theRook',
  subtitle: 'itMovesInStraightLines',
  image: util.assetUrl + 'images/learn/pieces/R.svg',
  intro: 'rookIntro',
  illustration: util.pieceImg('rook'),
  levels: [
    {
      goal: 'rookGoal',
      fen: '9/9/9/9/9/9/9/4R4/9 b -',
      apples: 'e6',
      nbMoves: 1,
      shapes: [arrow('e2e6')],
      noPocket: true,
    },
    {
      goal: 'grabAllTheStarsRemember',
      fen: '9/9/9/9/9/9/9/9/9 b R',
      apples: 'c3 g3',
      nbMoves: 3,
      noPocket: false,
      doNotShowPawnsInPocket: true,
    },
    {
      goal: 'rookPromotion',
      fen: '9/7R1/9/9/9/9/9/9/9 b -',
      apples: 'h2 g1 g8 f7',
      nbMoves: 4,
      noPocket: true,
    },
    {
      goal: 'rookSummary',
      fen: '9/9/9/9/4R4/9/9/9/9 b -',
      apples: 'e1',
      nbMoves: 1,
      shapes: [arrow('e5e9', 'green'), arrow('e5i5', 'green'), arrow('e5e1', 'green'), arrow('e5a5', 'green')],
      noPocket: true,
    },
    {
      goal: 'dragonSummaryTwo',
      fen: '9/9/9/9/4+R4/9/9/9/9 b -',
      apples: 'e8 d7',
      nbMoves: 2,
      shapes: [
        arrow('e5e9', 'green'),
        arrow('e5i5', 'green'),
        arrow('e5e1', 'green'),
        arrow('e5a5', 'green'),
        circle('e6'),
        circle('f6'),
        circle('f5'),
        circle('f4'),
        circle('e4'),
        circle('d4'),
        circle('d5'),
        circle('d6'),
      ],
      noPocket: true,
    },
  ].map(function (l, i) {
    return util.toLevel(l, i);
  }),
  complete: 'rookComplete',
};
