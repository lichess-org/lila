var util = require('../util');

module.exports = {
  title: 'The rook',
  subtitle: 'It moves in straight lines.',
  image: util.assetUrl + 'images/learn/pieces/R.svg',
  stages: [{
    goal: 'Click on the rook<br>to bring it to the castle!',
    fen: '8/8/8/8/8/8/4R3/8 w - - 0 1',
    items: {
      e7: 'flower'
    },
    nbMoves: 1,
    shapes: [{
      brush: 'paleGreen',
      orig: 'e2',
      dest: 'e7'
    }]
  }, {
    goal: 'Grab the star,<br>then go to the castle!',
    fen: '8/2R5/8/8/8/8/8/8 w - - 0 1',
    items: {
      c5: 'apple',
      a3: 'flower'
    },
    nbMoves: 3
  }, {
    goal: 'Grab the stars,<br>then go to the castle!',
    fen: '8/5R2/8/8/8/8/8/8 w - - 0 1',
    items: {
      a1: 'apple',
      a2: 'apple',
      c5: 'apple',
      b7: 'flower'
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
