var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/rally-the-troops.svg';

module.exports = {
  key: 'setup',
  title: 'Board setup',
  subtitle: 'How the game starts',
  image: imgUrl,
  intro: 'The two armies face each other, ready for the battle.',
  illustration: m('img.bg', {
    src: imgUrl
  }),
  levels: [{ // rook
    goal: 'This is the initial position<br>of every game of chess!',
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1'
  }, {
    goal: 'First place the rooks!<br>They go in the corners.',
    fen: 'r6r/8/8/8/8/8/8/2RR4 w - - 0 1',
    apples: 'a1 h1',
    nbMoves: 2,
    shapes: [arrow('c1a1'), arrow('d1h1')]
  }, {
    goal: 'Then place the knights!<br>They go next to the rooks.',
    fen: 'rn4nr/8/8/8/8/8/2NN4/R6R w - - 0 1',
    apples: 'b1 g1',
    nbMoves: 4
  }, {
    goal: 'Place the bishops!<br>They go next to the knights.',
    fen: 'rnb2bnr/8/8/8/4BB2/8/8/RN4NR w - - 0 1',
    apples: 'c1 f1',
    nbMoves: 4
  }, {
    goal: 'Place the queen!<br>It goes on its own color.',
    fen: 'rnbq1bnr/8/8/8/8/4Q3/8/RNB2BNR w - - 0 1',
    apples: 'd1',
    nbMoves: 2
  }, {
    goal: 'Place the king!<br>The last and easiet one.',
    fen: 'rnbqkbnr/8/8/8/8/4K3/8/RNBQ1BNR w - - 0 1',
    apples: 'e1',
    nbMoves: 2
  }, {
    goal: 'Pawns form the front line',
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1'
  }].map(function(l, i) {
    return util.toLevel(l, i);
  }),
  complete: 'Congratulations! You know how to setup the chess board.'
};
;
