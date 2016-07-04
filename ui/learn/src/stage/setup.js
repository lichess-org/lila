var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var and = assert.and;
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/rally-the-troops.svg';

module.exports = {
  key: 'setup',
  title: 'Board setup',
  subtitle: 'How the game starts',
  image: imgUrl,
  intro: 'The two armies face each other, ready for the battle.',
  illustration: util.roundSvg(imgUrl),
  levels: [{ // rook
    goal: 'This is the initial position<br>of every game of chess!<br>Make any move to continue.',
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - -',
    nbMoves: 1
  }, {
    goal: 'First place the rooks!<br>They go in the corners.',
    fen: 'r6r/8/8/8/8/8/8/2RR4 w - -',
    apples: 'a1 h1',
    nbMoves: 2,
    shapes: [arrow('c1a1'), arrow('d1h1')],
    success: [and(assert.pieceOn('R', 'a1'), assert.pieceOn('R', 'h1'))]
  }, {
    goal: 'Then place the knights!<br>They go next to the rooks.',
    fen: 'rn4nr/8/8/8/8/8/2NN4/R6R w - -',
    apples: 'b1 g1',
    nbMoves: 4,
    success: [and(assert.pieceOn('N', 'b1'), assert.pieceOn('N', 'g1'))]
  }, {
    goal: 'Place the bishops!<br>They go next to the knights.',
    fen: 'rnb2bnr/8/8/8/4BB2/8/8/RN4NR w - -',
    apples: 'c1 f1',
    nbMoves: 4,
    success: [and(assert.pieceOn('B', 'c1'), assert.pieceOn('B', 'f1'))]
  }, {
    goal: 'Place the queen!<br>It goes on its own color.',
    fen: 'rnbq1bnr/8/8/8/5Q2/8/8/RNB2BNR w - -',
    apples: 'd1',
    nbMoves: 2,
    success: [assert.pieceOn('Q', 'd1')]
  }, {
    goal: 'Place the king!<br>Right next to his queen.',
    fen: 'rnbqkbnr/8/8/8/5K2/8/8/RNBQ1BNR w - -',
    apples: 'e1',
    nbMoves: 3,
    success: [assert.pieceOn('K', 'e1')]
  }, {
    goal: 'Pawns form the front line.<br>Make any move to continue.',
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - -',
    nbMoves: 1,
    cssClass: 'highlight-2nd-rank highlight-7th-rank'
  }].map(function(l, i) {
    return util.toLevel(l, i);
  }),
  complete: 'Congratulations! You know how to setup the chess board.',
  cssClass: 'no-go-home'
};
;
