var item = require('../../item').builder;

module.exports = {
  goal: 'Grab the stars, then go to the castle!',
  color: 'white',
  fen: '8/8/8/8/8/8/4R3/8 w - - 0 1',
  items: {
    a1: item.apple(),
    a2: item.apple(),
    c5: item.apple(),
    g7: item.flower()
  }
};
