var m = require('mithril');
var util = require('../util');
var arrow = util.arrow;

module.exports = {
  key: 'king',
  title: 'The king',
  subtitle: 'The most important piece',
  image: util.assetUrl + 'images/learn/pieces/K.svg',
  intro: 'You are the king. If you fall in battle, the game is lost.',
  illustration: m('div.is2d.no-square',
    m('piece.king.white')
  ),
  levels: [{
    goal: 'The king is slow.',
    fen: '8/8/8/8/8/3K4/8/8 w - - 0 1',
    apples: 'e6',
    nbMoves: 3,
    shapes: [arrow('d3d4'), arrow('d4d5'), arrow('d5e6')]
  }, {
    goal: 'Grab all the stars!',
    fen: '8/8/8/8/8/8/8/4k3 b - - 0 1',
    apples: 'c2 d3 e2 e3',
    nbMoves: 4
  }, {
    goal: 'Last one!',
    fen: '8/8/8/4K3/8/8/8/8 w - - 0 1',
    apples: 'b5 c5 d6 e3 f3 g4',
    nbMoves: 8
  }].map(function(s, i) {
    s = util.toLevel(s, i);
    s.emptyApples = true;
    return s;
  }),
  complete: 'You can now command the commander!'
};
