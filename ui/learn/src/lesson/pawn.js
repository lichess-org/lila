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
    goal: 'The pawn can move 2 squares,<br>only on its first move!',
    fen: '8/8/8/8/8/8/3P4/8 w - - 0 1',
    items: {
      d6: 'flower'
    },
    nbMoves: 3,
    shapes: [{
      brush: 'paleGreen',
      orig: 'd2',
      dest: 'd4'
    }, {
      brush: 'paleGreen',
      orig: 'd4',
      dest: 'd5'
    }, {
      brush: 'paleGreen',
      orig: 'd5',
      dest: 'd6'
    }]
  }, {
    goal: 'Grab all these stars<br>as fast as possible!',
    fen: '8/8/8/8/8/4PP2/PPP4P/8 w - - 0 1',
    items: {
      a3: 'apple',
      b4: 'apple',
      c5: 'apple',
      e4: 'apple',
      f5: 'apple',
      h6: 'flower'
    },
    nbMoves: 10
  }].map(util.toStage),
  complete: 'Congratulations! Pawns have no secrets for you.'
};
