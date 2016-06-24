var util = require('../util');

module.exports = {
  title: 'The pawn',
  subtitle: 'It moves forward only.',
  image: util.assetUrl + 'images/learn/pieces/P.svg',
  stages: [{
    goal: 'Pawns move one square only.<br><br>But when they reach the other side of the board, they become a stronger piece!',
    fen: '8/8/8/P7/8/8/8/8 w - - 0 1',
    items: {
      f3: 'flower'
    },
    nbMoves: 4,
    shapes: [{
      brush: 'paleGreen',
      orig: 'a5',
      dest: 'a6'
    }, {
      brush: 'paleGreen',
      orig: 'a6',
      dest: 'a7'
    }, {
      brush: 'paleGreen',
      orig: 'a7',
      dest: 'a8'
    }, {
      brush: 'paleGreen',
      orig: 'a8',
      dest: 'f3'
    }]
  }, {
    goal: 'Most of the time, promoting to a queen is the best.<br><br>But sometimes a knight can come in handy!',
    fen: '8/8/8/5P2/8/8/8/8 w - - 0 1',
    items: {
      b6: 'apple',
      c4: 'apple',
      d7: 'apple',
      e5: 'apple',
      a8: 'flower'
    },
    nbMoves: 8
  }, {
    goal: 'A pawn on the second rank can move 2 squares at once!',
    fen: '8/8/8/8/8/8/4P3/8 w - - 0 1',
    items: {
      e6: 'flower'
    },
    nbMoves: 10,
    shapes: [{
      brush: 'paleGreen',
      orig: 'e2',
      dest: 'e4'
    }, {
      brush: 'paleGreen',
      orig: 'e4',
      dest: 'e5'
    }, {
      brush: 'paleGreen',
      orig: 'e5',
      dest: 'e6'
    }]
  }, {
    goal: 'Promote as fast as possible!',
    fen: '8/8/8/8/8/8/6P1/8 w - - 0 1',
    items: {
      a6: 'apple',
      a7: 'apple',
      b6: 'apple',
      b7: 'apple',
      b8: 'apple',
      c7: 'apple',
      c8: 'apple',
      a8: 'flower'
    },
    nbMoves: 13,
    shapes: [{
      brush: 'paleGreen',
      orig: 'g2',
      dest: 'g4'
    }]
  }].map(util.toStage),
  complete: 'Congratulations! Pawns have no secrets for you.'
};
