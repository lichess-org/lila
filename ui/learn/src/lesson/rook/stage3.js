var item = require('../../item').builder;

module.exports = {
  goal: 'Grab the stars,<br>then go to the castle!',
  color: 'white',
  fen: '8/5R2/8/8/8/8/8/8 w - - 0 1',
  items: {
    a1: item.apple(),
    a2: item.apple(),
    c5: item.apple(),
    b7: item.flower()
  },
  nbMoves: 7
};
