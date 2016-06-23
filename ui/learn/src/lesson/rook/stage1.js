var item = require('../../item').builder;

module.exports = {
  goal: 'Go to the castle!',
  color: 'white',
  fen: '8/8/8/8/8/8/4R3/8 w - - 0 1',
  items: {
    g7: item.flower()
  },
  nbMoves: 2
};
