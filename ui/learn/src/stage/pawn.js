var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

module.exports = {
  key: 'pawn',
  title: 'The pawn',
  subtitle: 'It moves forward only',
  image: util.assetUrl + 'images/learn/pieces/P.svg',
  intro: "Pawns are weak, but they pack a lot of potential.",
  illustration: util.pieceImg('pawn'),
  levels: [{
    goal: 'Pawns move one square only.<br>But when they reach the other side of the board, they become a stronger piece!',
    fen: '8/8/8/P7/8/8/8/8 w - -',
    apples: 'f3',
    nbMoves: 4,
    shapes: [arrow('a5a6'),arrow('a6a7'),arrow('a7a8'),arrow('a8f3')],
    explainPromotion: true
  }, {
    goal: 'Most of the time, promoting to a queen is the best.<br>But sometimes a knight can come in handy!',
    fen: '8/8/8/5P2/8/8/8/8 w - -',
    apples: 'b6 c4 d7 e5 a8',
    nbMoves: 8
  }, {
    goal: 'Pawns move forward,<br>but capture diagonally!',
    fen: '8/8/8/8/8/4P3/8/8 w - -',
    apples: 'c6 d5 d7',
    nbMoves: 4,
    shapes: [arrow('e3e4'),arrow('e4d5'),arrow('d5c6'),arrow('c6d7')],
    failure: [assert.noPieceOn('e3 e4 c6 d5 d7')]
  }, {
    goal: 'Capture, then promote!',
    fen: '8/8/8/8/8/1P6/8/8 w - -',
    apples: 'b4 b6 c4 c6 c7 d6',
    nbMoves: 8,
  }, {
    goal: 'Capture, then promote!',
    fen: '8/8/8/8/8/3P4/8/8 w - -',
    apples: 'c4 b5 b6 d5 d7 e6 c8',
    failure: [assert.whitePawnOnAnyOf('b5 d4 d6 c7')],
    nbMoves: 8
  }, {
    goal: 'Use all the pawns!<br>No need to promote.',
    fen: '8/8/p3pp1p/8/8/8/8/8 b - -',
    apples: 'b5 d4 e5 f4 g4',
    nbMoves: 7
  }, {
    goal: 'A pawn on the second rank can move 2 squares at once!',
    fen: '8/8/8/8/8/8/4P3/8 w - -',
    apples: 'd6',
    nbMoves: 3,
    shapes: [arrow('e2e4')],
    failure: [assert.whitePawnOnAnyOf('e3')],
    cssClass: 'highlight-2nd-rank'
  }, {
    goal: 'Grab all the stars!<br>No need to promote.',
    fen: '8/8/8/8/8/8/2PPPP2/8 w - -',
    apples: 'c5 d5 e5 f5 d3 e4',
    nbMoves: 9
  }].map(util.toLevel),
  complete: 'Congratulations! Pawns have no secrets for you.'
};
