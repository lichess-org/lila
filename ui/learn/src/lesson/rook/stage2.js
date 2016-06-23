var item = require('../../item').builder;

module.exports = {
  goal: 'Grab the star,<br>then go to the castle!',
  color: 'white',
  fen: '8/2R5/8/8/8/8/8/8 w - - 0 1',
  items: {
    c5: item.apple(),
    a3: item.flower()
  },
  nbMoves: 3
};
