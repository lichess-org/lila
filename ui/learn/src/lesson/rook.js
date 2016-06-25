var util = require('../util');
var arrow = util.arrow;

module.exports = {
  title: 'The rook',
  subtitle: 'It moves in straight lines.',
  image: util.assetUrl + 'images/learn/pieces/R.svg',
  stages: [{
    goal: 'Click on the rook<br>to bring it to the star!',
    fen: '8/8/8/8/8/8/4R3/8 w - - 0 1',
    apples: ['e7'],
    nbMoves: 1,
    shapes: [arrow('e2e7')]
  }, {
    goal: 'Grab all the stars!',
    fen: '8/2R5/8/8/8/8/8/8 w - - 0 1',
    apples: ['c5', 'g5'],
    nbMoves: 3,
    shapes: [arrow('c7c5'), arrow('c5g5')]
  }, {
    goal: 'The fewer moves you make,<br>the more points you win!',
    fen: '8/5R2/8/8/8/8/8/8 w - - 0 1',
    items: {
      a1: 'apple',
      a7: 'apple',
      e1: 'apple',
      e8: 'apple',
      g7: 'apple',
      g8: 'apple',
      g1: 'flower'
    },
    nbMoves: 7
  }, {
    goal: 'Rooks don\'t like diagonals!',
    fen: '8/8/8/8/8/8/8/R7 w - - 0 1',
    items: {
      b4: 'apple',
      c5: 'apple',
      d6: 'apple',
      f8: 'flower'
    },
    nbMoves: 8
  }, {
    goal: 'Use two rooks<br>to speed things up!',
    fen: '8/8/8/8/8/5R2/8/R7 w - - 0 1',
    items: {
      a6: 'apple',
      b8: 'apple',
      h3: 'apple',
      h4: 'apple',
      d4: 'flower'
    },
    nbMoves: 6
  }].map(util.toStage),
  complete: 'Congratulations! You have successfully mastered the rook.'
};
