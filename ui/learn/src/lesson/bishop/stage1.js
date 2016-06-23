var item = require('../../item').builder;

module.exports = {
  goal: 'Go to the castle!',
  color: 'white',
  fen: '8/8/8/8/8/5B2/8/8 w - - 0 1',
  items: {
    b7: item.flower()
  },
  nbMoves: 1,
  shapes: [{
    brush: 'paleGreen',
    orig: 'f3',
    dest: 'b7'
  }]
};
