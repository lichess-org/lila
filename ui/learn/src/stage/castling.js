var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var and = assert.and, not = assert.not;
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/castle.svg';

var castledKingSide = assert.lastMoveSan('O-O');
var castledQueenSide = assert.lastMoveSan('O-O-O');

module.exports = {
  key: 'castling',
  title: 'Castling',
  subtitle: 'The special king move',
  image: imgUrl,
  intro: 'Bring your king to safety, and deploy your rook for attack!',
  illustration: util.roundSvg(imgUrl),
  levels: [{
    goal: 'Move your king two squares<br>to castle king side!',
    fen: 'rnbqkbnr/pppppppp/8/8/2B5/4PN2/PPPP1PPP/RNBQK2R w KQkq -',
    nbMoves: 1,
    shapes: [arrow('e1g1')],
    failure: [assert.pieceNotOn('K', 'g1')]
  }, {
    goal: 'Move your king two squares<br>to castle queen side!',
    fen: 'rnbqkbnr/pppppppp/8/8/4P3/1PN5/PBPPQPPP/R3KBNR w KQkq -',
    nbMoves: 1,
    shapes: [arrow('e1c1')],
    failure: [assert.pieceNotOn('K', 'c1')]
  }, {
    goal: 'The knight is in the way!<br>Move it, then castle king-side.',
    fen: 'rnbqkbnr/pppppppp/8/8/8/4P3/PPPPBPPP/RNBQK1NR w KQkq -',
    nbMoves: 2,
    shapes: [arrow('e1g1'), arrow('g1f3')],
    success: [castledKingSide],
    failure: [and(not(castledKingSide), assert.pieceNotOn('K', 'e1'))]
  }, {
    goal: 'Castle king-side!<br>You need move out pieces first.',
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -',
    nbMoves: 4,
    shapes: [arrow('e1g1')],
    success: [castledKingSide],
    failure: [and(not(castledKingSide), assert.pieceNotOn('K', 'e1'))]
  }, {
    goal: 'Castle queen-side!<br>You need move out pieces first.',
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -',
    nbMoves: 5,
    shapes: [arrow('e1c1')],
    success: [castledQueenSide],
    failure: [and(not(castledQueenSide), assert.pieceNotOn('K', 'e1'))]
  }].map(function(l, i) {
    l.autoCastle = true;
    return util.toLevel(l, i);
  }),
  complete: 'Congratulations! You can command a bishop.'
};

