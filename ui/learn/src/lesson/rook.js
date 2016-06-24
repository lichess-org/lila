var util = require('../util');
var item = require('../item').builder;

module.exports = {
  title: 'The rook',
  subtitle: 'It moves in straight lines.',
  image: util.assetUrl + 'images/learn/pieces/R.svg',
  stages: [{
    goal: 'Click on the rook<br>to bring it to the castle!',
    fen: '8/8/8/8/8/8/4R3/8 w - - 0 1',
    items: {
      e7: item.flower()
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
      c5: item.apple(),
      a3: item.flower()
    },
    nbMoves: 3
  }, {
    goal: 'Grab the stars,<br>then go to the castle!',
    fen: '8/5R2/8/8/8/8/8/8 w - - 0 1',
    items: {
      a1: item.apple(),
      a2: item.apple(),
      c5: item.apple(),
      b7: item.flower()
    },
    nbMoves: 7
  }, {
    goal: 'Rooks don\'t like diagonals!',
    fen: '8/8/8/8/8/8/8/R7 w - - 0 1',
    items: {
      b4: item.apple(),
      c5: item.apple(),
      d6: item.apple(),
      f8: item.flower()
    },
    nbMoves: 8
  }, {
    goal: 'Use two rooks<br>to speed things up!',
    fen: '8/8/8/8/8/5R2/8/R7 w - - 0 1',
    items: {
      a6: item.apple(),
      b8: item.apple(),
      h3: item.apple(),
      h4: item.apple(),
      d4: item.flower()
    },
    nbMoves: 6
  }].map(util.toStage),
  complete: 'Congratulations! You have successfully mastered the rook.'
};
