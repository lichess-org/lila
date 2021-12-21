var util = require('../util');
var arrow = util.arrow;
var circle = util.circle;

module.exports = {
  key: 'knight',
  title: 'theKnight',
  subtitle: 'itMovesInAnLShape',
  image: util.assetUrl + 'images/learn/pieces/N.svg',
  intro: 'knightIntro',
  illustration: util.pieceImg('knight'),
  levels: [
    {
      goal: 'knightsHaveAFancyWay',
      fen: '9/9/9/9/9/9/9/9/1N7 b -',
      apples: 'c3 b5',
      nbMoves: 2,
      shapes: [arrow('b1c3'), arrow('c3b5')],
    },
    {
      goal: 'knightPromotion',
      fen: '9/9/9/9/9/9/9/1N7/9 b -',
      apples: 'c4 b6 c8 d8 d7',
      nbMoves: 5,
    },
    {
      goal: 'knightSummary',
      fen: '9/9/9/9/9/4N4/9/9/9 b -',
      apples: 'f6',
      nbMoves: 1,
      shapes: [circle('f6'), circle('d6')],
    },
    {
      goal: 'pknightSummary',
      fen: '9/9/9/9/9/4+N4/9/9/9 b -',
      apples: 'e6',
      nbMoves: 2,
      shapes: [circle('d4'), circle('d5'), circle('e5'), circle('f5'), circle('f4'), circle('e3')],
    },
  ].map(function (l, i) {
    l.noPocket = true;
    return util.toLevel(l, i);
  }),
  complete: 'knightComplete',
};
