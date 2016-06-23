var item = require('../../item').builder;

module.exports = {
  goal: 'Grab the star,<br>then go to the castle!',
  color: 'white',
  fen: '8/8/8/8/8/8/8/2B5 w - - 0 1',
  items: {
    h6: item.apple(),
    a1: item.flower()
  },
  nbMoves: 3
};
