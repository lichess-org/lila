var item = require('../../item').builder;

module.exports = {
  goal: 'Rooks don\'t like diagonals!',
  color: 'white',
  fen: '8/8/8/8/8/8/8/R7 w - - 0 1',
  items: {
    b4: item.apple(),
    c5: item.apple(),
    d6: item.apple(),
    f8: item.flower()
  },
  nbMoves: 8
};
