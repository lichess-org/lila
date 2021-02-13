var util = require('../util');
var assert = require('../assert');
var and = assert.and;
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/rally-the-troops.svg';

module.exports = {
  key: 'setup',
  title: 'boardSetup',
  subtitle: 'howTheGameStarts',
  image: imgUrl,
  intro: 'boardSetupIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      // rook
      goal: 'thisIsTheInitialPosition',
      fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - -',
      nbMoves: 1,
    },
    {
      goal: 'firstPlaceTheRooks',
      fen: 'r6r/pppppppp/8/8/8/8/8/2RR4 w - -',
      apples: 'a1 h1',
      nbMoves: 2,
      shapes: [arrow('c1a1'), arrow('d1h1')],
      success: and(assert.pieceOn('R', 'a1'), assert.pieceOn('R', 'h1')),
    },
    {
      goal: 'thenPlaceTheKnights',
      fen: 'rn4nr/pppppppp/8/8/8/8/2NN4/R6R w - -',
      apples: 'b1 g1',
      nbMoves: 4,
      success: and(assert.pieceOn('N', 'b1'), assert.pieceOn('N', 'g1')),
    },
    {
      goal: 'placeTheBishops',
      fen: 'rnb2bnr/pppppppp/8/8/4BB2/8/8/RN4NR w - -',
      apples: 'c1 f1',
      nbMoves: 4,
      success: and(assert.pieceOn('B', 'c1'), assert.pieceOn('B', 'f1')),
    },
    {
      goal: 'placeTheQueen',
      fen: 'rnbq1bnr/pppppppp/8/8/5Q2/8/8/RNB2BNR w - -',
      apples: 'd1',
      nbMoves: 2,
      success: assert.pieceOn('Q', 'd1'),
    },
    {
      goal: 'placeTheKing',
      fen: 'rnbqkbnr/pppppppp/8/8/5K2/8/8/RNBQ1BNR w - -',
      apples: 'e1',
      nbMoves: 3,
      success: assert.pieceOn('K', 'e1'),
    },
    {
      goal: 'pawnsFormTheFrontLine',
      fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - -',
      nbMoves: 1,
      cssClass: 'highlight-2nd-rank highlight-7th-rank',
    },
  ].map(function (l, i) {
    return util.toLevel(l, i);
  }),
  complete: 'boardSetupComplete',
  cssClass: 'no-go-home',
};
