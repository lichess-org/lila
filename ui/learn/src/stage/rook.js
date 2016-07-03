var m = require('mithril');
var util = require('../util');
var arrow = util.arrow;

module.exports = {
  key: 'rook',
  title: 'The rook',
  subtitle: 'It moves in straight lines.',
  image: util.assetUrl + 'images/learn/pieces/R.svg',
  intro: 'The rook is a powerful piece. Are you ready to learn how to use it?',
  illustration: m('div.is2d.no-square',
    m('piece.rook.white')
  ),
  levels: [{
    goal: 'Click on the rook<br>to bring it to the star!',
    fen: '8/8/8/8/8/8/4R3/8 w - - 0 1',
    apples: 'e7',
    nbMoves: 1,
    shapes: [arrow('e2e7')]
  }, {
    goal: 'Grab all the stars!',
    fen: '8/2R5/8/8/8/8/8/8 w - - 0 1',
    apples: 'c5 g5',
    nbMoves: 2,
    shapes: [arrow('c7c5'), arrow('c5g5')]
  }, {
    goal: 'The fewer moves you make,<br>the more points you win!',
    fen: '8/8/8/8/3R4/8/8/8 w - - 0 1',
    apples: 'a4 g3 g4',
    nbMoves: 3
  }, {
    goal: 'Grab all the stars!',
    fen: '7R/8/8/8/8/8/8/8 w - - 0 1',
    apples: 'f8 g1 g7 g8 h7',
    nbMoves: 5
  }, {
    goal: 'Use two rooks<br>to speed things up!',
    fen: '8/1R6/8/8/3R4/8/8/8 w - - 0 1',
    apples: 'a4 g3 g7 h4',
    nbMoves: 11
  }, {
    goal: 'Use two rooks<br>to speed things up!',
    fen: '8/8/8/8/8/5R2/8/R7 w - - 0 1',
    apples: 'b7 d1 d5 f2 f7 g4 g7',
    nbMoves: 7
  }].map(util.toLevel),
  complete: 'Congratulations! You have successfully mastered the rook.'
};
