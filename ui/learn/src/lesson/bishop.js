var util = require('../util');
var item = require('../item').builder;

module.exports = {
  title: 'The bishop',
  subtitle: 'It moves diagonally.',
  image: util.assetUrl + 'images/learn/pieces/B.svg',
  stages: [{
    goal: 'Go to the castle!',
    fen: '8/8/8/8/8/5B2/8/8 w - - 0 1',
    items: {
      b7: item.flower
    },
    nbMoves: 1,
    shapes: [{
      brush: 'paleGreen',
      orig: 'f3',
      dest: 'b7'
    }]
  }, {
    goal: 'Grab the star,<br>then go to the castle!',
    fen: '8/8/8/8/8/8/8/2B5 w - - 0 1',
    items: {
      h6: item.apple,
      a1: item.flower
    },
    nbMoves: 3
  }, {
    goal: 'Grab the stars,<br>then go to the castle!',
    fen: '4B3/8/8/8/8/8/8/8 w - - 0 1',
    items: {
      a4: item.apple,
      b7: item.apple,
      c6: item.apple,
      d1: item.apple,
      h5: item.apple,
      a8: item.flower
    },
    nbMoves: 6
  }].map(util.toStage),
  complete: 'Congratulations! You can command a bishop.'
};
