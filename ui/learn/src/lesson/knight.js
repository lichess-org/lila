var util = require('../util');
var arrow = util.arrow;

module.exports = {
  title: 'The knight',
  subtitle: 'It moves in a L shape.',
  image: util.assetUrl + 'images/learn/pieces/N.svg',
  stages: [{
    goal: 'Knights have a fancy way<br>of jumping around!',
    fen: '8/8/8/8/4N3/8/8/8 w - - 0 1',
    items: {
      c5: 'apple',
      d7: 'flower'
    },
    nbMoves: 2,
    shapes: [arrow('e4c5'), arrow('c5d7')]
  }, {
    goal: 'Get the hang of it!',
    fen: '8/8/8/8/8/8/8/1N6 w - - 0 1',
    items: {
      c3: 'apple',
      d4: 'apple',
      e2: 'apple',
      e5: 'apple',
      f3: 'apple',
      g4: 'apple',
      f6: 'apple',
      h5: 'apple',
      g7: 'flower'
    },
    nbMoves: 10
  }, {
    goal: 'Knights don\'t like long distances!',
    fen: '8/8/8/8/8/8/8/N7 w - - 0 1',
    items: {
      a8: 'apple',
      g7: 'apple',
      g2: 'flower'
    },
    nbMoves: 11
  }, {
    goal: 'Star party!',
    fen: '1n6/8/8/8/8/8/8/8 b - - 0 1',
    items: {
      a4: 'apple',
      a5: 'apple',
      a6: 'apple',
      b4: 'apple',
      b6: 'apple',
      c4: 'apple',
      c5: 'apple',
      c6: 'apple',
      e6: 'apple',
      e7: 'apple',
      e8: 'apple',
      f6: 'apple',
      f8: 'apple',
      g6: 'apple',
      g7: 'apple',
      g8: 'apple',
      f2: 'apple',
      f3: 'apple',
      f4: 'apple',
      g2: 'apple',
      g4: 'apple',
      h2: 'apple',
      h3: 'apple',
      h4: 'apple',
      d2: 'flower'
    },
    nbMoves: 28
  }, {
    goal: 'Does this look easy?<br>It\'s not :)',
    fen: '1N6/8/8/8/8/8/8/8 w - - 0 1',
    items: {
      b7: 'apple',
      d5: 'apple',
      f3: 'apple',
      h1: 'flower'
    },
    nbMoves: 15
  }].map(util.toStage),
  complete: 'Congratulations! You have mastered the knight.'
};
