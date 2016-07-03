var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/winged-sword.svg';

var oneMove =  'Attack the opponent king<br>in one move!';

module.exports = {
  key: 'check',
  title: 'Check',
  subtitle: 'Attack the opponent king.',
  image: imgUrl,
  intro: 'To check your opponent, attack their king. They must defend it!',
  illustration: m('img.bg', {
    src: imgUrl
  }),
  levels: [{ // rook
    goal: oneMove,
    fen: '4k3/8/2b5/8/8/8/8/R7 w - - 0 1',
    nbMoves: 1,
    shapes: [arrow('a1e1')],
    success: [assert.check]
  }, { // queen
    goal: oneMove,
    fen: '8/8/4k3/3n4/8/1Q6/8/8 w - - 0 1',
    nbMoves: 1,
    success: [assert.check]
  }, { // bishop
    goal: oneMove,
    fen: '3qk3/1pp5/3p4/4p3/8/3B4/6r1/8 w - - 0 1',
    nbMoves: 1,
    success: [assert.check]
  }, { // pawn
    goal: oneMove,
    fen: '8/3pp1b1/2n5/2q5/4K3/8/2N5/5Q2 b - - 0 1',
    nbMoves: 1,
    success: [assert.check]
  }, { // knight
    goal: oneMove,
    fen: '8/2b1q2n/1ppk4/2N5/8/8/8/8 w - - 0 1',
    nbMoves: 1,
    success: [assert.check]
  }, {
    // R+Q
    goal: oneMove,
    fen: '8/8/8/8/2q5/5N2/1R3K2/r7 b - - 0 1',
    nbMoves: 1,
    success: [assert.check]
  }, {
    // many pieces
    goal: oneMove,
    fen: '3q4/5r2/8/2Bn4/4N3/8/3K4/R7 b - - 0 1',
    nbMoves: 1,
    success: [assert.check]
  }].map(util.toLevel),
  complete: 'Congratulations! You checked your opponent, forcing them to defend their king!'
};
