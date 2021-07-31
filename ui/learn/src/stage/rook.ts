const util = require('../util');
const arrow = util.arrow;

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
      fen: '8/8/8/8/8/8/4R3/8 w - -',
      apples: 'e7',
      nbMoves: 1,
      shapes: [arrow('e2e7')],
    },
    {
      goal: 'grabAllTheStars',
      fen: '8/2R5/8/8/8/8/8/8 w - -',
      apples: 'c5 g5',
      nbMoves: 2,
      shapes: [arrow('c7c5'), arrow('c5g5')],
    },
    {
      goal: 'theFewerMoves',
      fen: '8/8/8/8/3R4/8/8/8 w - -',
      apples: 'a4 g3 g4',
      nbMoves: 3,
    },
    {
      goal: 'theFewerMoves',
      fen: '7R/8/8/8/8/8/8/8 w - -',
      apples: 'f8 g1 g7 g8 h7',
      nbMoves: 5,
    },
    {
      goal: 'useTwoRooks',
      fen: '8/1R6/8/8/3R4/8/8/8 w - -',
      apples: 'a4 g3 g7 h4',
      nbMoves: 4,
    },
    {
      goal: 'useTwoRooks',
      fen: '8/8/8/8/8/5R2/8/R7 w - -',
      apples: 'b7 d1 d5 f2 f7 g4 g7',
      nbMoves: 7,
    },
  ].map(util.toLevel),
  complete: 'rookComplete',
};
