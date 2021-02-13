var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

module.exports = {
  key: 'pawn',
  title: 'thePawn',
  subtitle: 'itMovesForwardOnly',
  image: util.assetUrl + 'images/learn/pieces/P.svg',
  intro: 'pawnIntro',
  illustration: util.pieceImg('pawn'),
  levels: [
    {
      goal: 'pawnsMoveOneSquareOnly',
      fen: '8/8/8/P7/8/8/8/8 w - -',
      apples: 'f3',
      nbMoves: 4,
      shapes: [arrow('a5a6'), arrow('a6a7'), arrow('a7a8'), arrow('a8f3')],
      explainPromotion: true,
    },
    {
      goal: 'mostOfTheTimePromotingToAQueenIsBest',
      fen: '8/8/8/5P2/8/8/8/8 w - -',
      apples: 'b6 c4 d7 e5 a8',
      nbMoves: 8,
    },
    {
      goal: 'pawnsMoveForward',
      fen: '8/8/8/8/8/4P3/8/8 w - -',
      apples: 'c6 d5 d7',
      nbMoves: 4,
      shapes: [arrow('e3e4'), arrow('e4d5'), arrow('d5c6'), arrow('c6d7')],
      failure: assert.noPieceOn('e3 e4 c6 d5 d7'),
    },
    {
      goal: 'captureThenPromote',
      fen: '8/8/8/8/8/1P6/8/8 w - -',
      apples: 'b4 b6 c4 c6 c7 d6',
      nbMoves: 8,
    },
    {
      goal: 'captureThenPromote',
      fen: '8/8/8/8/8/3P4/8/8 w - -',
      apples: 'c4 b5 b6 d5 d7 e6 c8',
      failure: assert.whitePawnOnAnyOf('b5 d4 d6 c7'),
      nbMoves: 8,
    },
    {
      goal: 'useAllThePawns',
      fen: '8/8/8/8/8/P1PP3P/8/8 w - -',
      apples: 'b5 c5 d4 e5 g4',
      nbMoves: 7,
    },
    {
      goal: 'aPawnOnTheSecondRank',
      fen: '8/8/8/8/8/8/4P3/8 w - -',
      apples: 'd6',
      nbMoves: 3,
      shapes: [arrow('e2e4')],
      failure: assert.whitePawnOnAnyOf('e3'),
      cssClass: 'highlight-2nd-rank',
    },
    {
      goal: 'grabAllTheStarsNoNeedToPromote',
      fen: '8/8/8/8/8/8/2PPPP2/8 w - -',
      apples: 'c5 d5 e5 f5 d3 e4',
      nbMoves: 9,
    },
  ].map(util.toLevel),
  complete: 'pawnComplete',
};
