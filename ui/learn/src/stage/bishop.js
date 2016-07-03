var m = require('mithril');
var util = require('../util');
var arrow = util.arrow;

module.exports = {
  key: 'bishop',
  title: 'The bishop',
  subtitle: 'It moves diagonally',
  image: util.assetUrl + 'images/learn/pieces/B.svg',
  intro: 'Next we will learn how to manoeuver a bishop!',
  illustration: m('div.is2d.no-square',
    m('piece.bishop.white')
  ),
  levels: [{
    goal: 'Grab all the stars!',
    fen: '8/8/8/8/8/5B2/8/8 w - - 0 1',
    apples: 'd5 g8',
    nbMoves: 2,
    shapes: [arrow('f3d5'), arrow('d5g8')]
  }, {
    goal: 'The fewer moves you make,<br>the more points you win!',
    fen: '8/8/8/8/8/1B6/8/8 w - - 0 1',
    apples: 'a2 b1 b5 d1 d3 e2',
    nbMoves: 6
  }, {
    goal: 'Grab all the stars!',
    fen: '8/8/8/8/3B4/8/8/8 w - - 0 1',
    apples: 'a1 b6 c1 e3 g7 h6',
    nbMoves: 6
  }, {
    goal: 'Grab all the stars!',
    fen: '8/8/8/8/2b5/8/8/8 b - - 0 1',
    apples: 'a4 b1 b3 c2 d3 e2',
    nbMoves: 6
  }, {
    goal: 'One light-squared bishop,<br>one dark-squared bishop.<br>You need both!',
    fen: '8/8/8/8/8/8/8/2b2b2 b - - 0 1',
    apples: 'd3 d4 d5 e3 e4 e5',
    nbMoves: 6
  }, {
    goal: 'One light-squared bishop,<br>one dark-squared bishop.<br>You need both!',
    fen: '8/3B4/8/8/8/2B5/8/8 w - - 0 1',
    apples: 'a3 c2 e7 f5 f6 g8 h4 h7',
    nbMoves: 11
  }].map(util.toLevel),
  complete: 'Congratulations! You can command a bishop.'
};
