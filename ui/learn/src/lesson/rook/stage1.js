var item = require('../../item').builder;

module.exports = {
  goal: 'Click on the rook<br>to bring it to the castle!',
  color: 'white',
  fen: '8/8/8/8/8/8/4R3/8 w - - 0 1',
  items: {
    e7: item.flower()
  },
  nbMoves: 1,
  shapes: [{
    brush: 'green',
    orig: 'e2',
    dest: 'e7'
  }]
};
