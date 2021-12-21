var util = require('../util');
var arrow = util.arrow;
var circle = util.circle;

module.exports = {
  key: 'lance',
  title: 'theLance',
  subtitle: 'itMovesStraightForward',
  image: util.assetUrl + 'images/learn/pieces/L.svg',
  intro: 'lanceIntro',
  illustration: util.pieceImg('lance'),
  levels: [
    {
      goal: 'lancesAreStraighforward',
      fen: '9/9/9/9/9/9/9/9/4L4 b -',
      apples: 'e3 e6',
      nbMoves: 2,
      shapes: [arrow('e1e3'), arrow('e3e6')],
    },
    {
      goal: 'lancePromotion',
      fen: '9/9/9/9/9/9/9/9/L7L b -',
      apples: 'a8 b9 i8 h9',
      nbMoves: 4,
    },
    {
      goal: 'lanceSummary',
      fen: '9/9/9/9/9/4L4/9/9/9 b -',
      apples: 'e6',
      nbMoves: 1,
      shapes: [arrow('e4e9', 'green')],
    },
    {
      goal: 'planceSummaryTwo',
      fen: '9/9/9/9/9/4+L4/9/9/9 b -',
      apples: 'e6',
      nbMoves: 2,
      shapes: [circle('d4'), circle('d5'), circle('e5'), circle('f5'), circle('f4'), circle('e3')],
    },
  ].map(function (l, i) {
    l.noPocket = true;
    return util.toLevel(l, i);
  }),
  complete: 'lanceComplete',
};
