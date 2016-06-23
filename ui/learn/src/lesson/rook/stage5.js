var item = require('../../item').builder;

module.exports = {
  goal: 'Use two rooks<br>to speed things up!',
  color: 'white',
  fen: '8/8/8/8/8/5R2/8/R7 w - - 0 1',
  items: {
    a6: item.apple(),
    b8: item.apple(),
    h3: item.apple(),
    h4: item.apple(),
    d4: item.flower()
  },
  nbMoves: 6
};
