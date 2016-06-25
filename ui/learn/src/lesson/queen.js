var util = require('../util');
var arrow = util.arrow;

module.exports = {
  title: 'The queen',
  subtitle: 'Queen = rook + bishop.',
  image: util.assetUrl + 'images/learn/pieces/Q.svg',
  stages: [{
    goal: 'Use the queen like a rook!',
    fen: '8/8/8/8/8/8/8/3Q4 w - - 0 1',
    items: {
      d8: 'apple',
      h8: 'flower'
    },
    nbMoves: 1,
    shapes: [arrow('d1d8'), arrow('d8h8')]
  }, {
    goal: 'Use the queen like a bishop!',
    fen: '8/8/8/8/8/8/8/3Q4 w - - 0 1',
    items: {
      h5: 'apple',
      e8: 'flower'
    },
    nbMoves: 1,
    shapes: [arrow('d1h5'), arrow('h5e8')]
  }, {
    goal: 'Grab the stars,<br>then go to the castle!',
    fen: '8/8/8/8/8/2Q5/8/8 w - - 0 1',
    items: {
      b2: 'apple',
      b7: 'apple',
      e7: 'apple',
      b7: 'apple',
      g5: 'apple',
      g8: 'apple',
      h8: 'apple',
      g2: 'flower'
    },
    nbMoves: 7
  }, {
    goal: 'The queen is the strong!',
    fen: '8/8/8/8/8/8/8/7Q w - - 0 1',
    items: {
      a3: 'apple',
      b2: 'apple',
      b3: 'apple',
      b7: 'apple',
      c3: 'apple',
      c6: 'apple',
      c7: 'apple',
      d7: 'apple',
      e5: 'apple',
      f4: 'apple',
      f5: 'apple',
      g5: 'apple',
      f8: 'flower'
    },
    nbMoves: 13
  }, {
    goal: 'Grab the stars,<br>then go to the castle!',
    fen: '8/8/8/8/8/8/8/Q7 w - - 0 1',
    items: {
      a5: 'apple',
      b3: 'apple',
      c1: 'apple',
      d7: 'apple',
      e2: 'apple',
      f8: 'apple',
      g6: 'apple',
      h4: 'apple',
      h8: 'flower'
    },
    nbMoves: 16
  }].map(util.toStage),
  complete: 'Congratulations! Queens have no secrets for you.'
};
