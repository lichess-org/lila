var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/spinning-blades.svg';

module.exports = {
  key: 'enpassant',
  title: 'En passant',
  subtitle: 'The special pawn move',
  image: imgUrl,
  intro: 'When the opponent pawn moved by two squares, you can take it like if it moved by one square.',
  illustration: util.roundSvg(imgUrl),
  levels: [{
    goal: 'Black just moved the pawn<br>by two squares!<br>Take it en passant.',
    fen: 'rnbqkbnr/pppppppp/8/2P5/8/8/PP1PPPPP/RNBQKBNR b KQkq -',
    color: 'white',
    nbMoves: 1,
    success: assert.scenarioComplete,
    failure: assert.scenarioFailed,
    detectCapture: false,
    scenario: [{
      move: 'd7d5',
      shapes: [arrow('c5d6')]
    }, 'c5d6'],
    captures: 1
  }, {
    goal: 'En passant only works<br>immediately after the opponent<br>moved the pawn.',
    fen: 'rnbqkbnr/ppp1pppp/8/2Pp3P/8/8/PP1PPPP1/RNBQKBNR b KQkq -',
    color: 'white',
    nbMoves: 1,
    success: assert.scenarioComplete,
    failure: assert.scenarioFailed,
    detectCapture: false,
    scenario: [{
      move: 'g7g5',
      shapes: [arrow('h5g6'), arrow('c5d6', 'red')]
    }, 'h5g6'],
    captures: 1
  }, {
    goal: 'En passant only works<br>if your pawn is on the 5th rank.',
    fen: 'rnbqkbnr/pppppppp/P7/2P5/8/8/PP1PPPP1/RNBQKBNR b KQkq -',
    color: 'white',
    nbMoves: 1,
    success: assert.scenarioComplete,
    failure: assert.scenarioFailed,
    detectCapture: false,
    scenario: [{
      move: 'b7b5',
      shapes: [arrow('c5b6'), arrow('a6b7', 'red')]
    }, 'c5b6'],
    captures: 1,
    cssClass: 'highlight-5th-rank'
  }, {
    goal: 'Take all the pawns en passant!',
    fen: 'rnbqkbnr/pppppppp/8/2PPP2P/8/8/PP1P1PP1/RNBQKBNR b KQkq -',
    color: 'white',
    nbMoves: 4,
    detectCapture: false,
    success: assert.scenarioComplete,
    failure: assert.scenarioFailed,
    scenario: [
      'b7b5',
      'c5b6',
      'f7f5',
      'e5f6',
      'c7c5',
      'd5c6',
      'g7g5',
      'h5g6'
    ],
    captures: 4
  }].map(util.toLevel),
  complete: 'Congratulations! You can now take en passant.'
};
