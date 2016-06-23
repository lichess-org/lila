var item = require('../../item').builder;

module.exports = {
  goal: 'Grab the stars,<br>then go to the castle!',
  color: 'white',
  fen: '4B3/8/8/8/8/8/8/8 w - - 0 1',
  items: {
    a4: item.apple(),
    b7: item.apple(),
    c6: item.apple(),
    d1: item.apple(),
    h5: item.apple(),
    a8: item.flower()
  },
  nbMoves: 6
};
