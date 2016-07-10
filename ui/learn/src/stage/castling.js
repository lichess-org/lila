var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow,
  circle = util.circle;

var imgUrl = util.assetUrl + 'images/learn/castle.svg';

var castledKingSide = assert.lastMoveSan('O-O');
var castledQueenSide = assert.lastMoveSan('O-O-O');
var cantCastleKingSide = assert.and(
  assert.not(castledKingSide),
  assert.or(assert.pieceNotOn('K', 'e1'), assert.pieceNotOn('R', 'h1'))
);
var cantCastleQueenSide = assert.and(
  assert.not(castledQueenSide),
  assert.or(assert.pieceNotOn('K', 'e1'), assert.pieceNotOn('R', 'a1'))
);

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
    success: castledKingSide,
    failure: cantCastleKingSide
  }, {
    goal: 'Move your king two squares<br>to castle queen side!',
    fen: 'rnbqkbnr/pppppppp/8/8/4P3/1PN5/PBPPQPPP/R3KBNR w KQkq -',
    nbMoves: 1,
    shapes: [arrow('e1c1')],
    success: castledQueenSide,
    failure: cantCastleQueenSide
  }, {
    goal: 'The knight is in the way!<br>Move it, then castle king-side.',
    fen: 'rnbqkbnr/pppppppp/8/8/8/4P3/PPPPBPPP/RNBQK1NR w KQkq -',
    nbMoves: 2,
    shapes: [arrow('e1g1'), arrow('g1f3')],
    success: castledKingSide,
    failure: cantCastleKingSide
  }, {
    goal: 'Castle king-side!<br>You need move out pieces first.',
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -',
    nbMoves: 4,
    shapes: [arrow('e1g1')],
    success: castledKingSide,
    failure: cantCastleKingSide
  }, {
    goal: 'Castle queen-side!<br>You need move out pieces first.',
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -',
    nbMoves: 6,
    shapes: [arrow('e1c1')],
    success: castledQueenSide,
    failure: cantCastleQueenSide
  }, {
    goal: 'You cannot castle if<br>the king has already moved<br>or the rook has already moved.',
    fen: 'rnbqkbnr/pppppppp/8/8/3P4/1PN1PN2/PBPQBPPP/R3K1R1 w Qkq -',
    nbMoves: 1,
    shapes: [arrow('e1g1', 'red'), arrow('e1c1')],
    success: castledQueenSide,
    failure: cantCastleQueenSide
  }, {
    goal: 'You cannot castle if<br>the king is attacked on the way.<br>Block the check then castle!',
    fen: 'rn1qkbnr/ppp1pppp/3p4/8/2b5/4PN2/PPPP1PPP/RNBQK2R w KQkq -',
    nbMoves: 2,
    shapes: [arrow('c4f1', 'red'), circle('e1'), circle('f1'), circle('g1')],
    success: castledKingSide,
    failure: cantCastleKingSide,
    detectCapture: false
  }, {
    goal: 'Find a way to<br>castle king-side!',
    fen: 'rnb2rk1/pppppppp/8/8/8/4Nb1n/PPPP1P1P/RNB1KB1R w KQkq -',
    nbMoves: 2,
    shapes: [arrow('e1g1')],
    success: castledKingSide,
    failure: cantCastleKingSide,
    detectCapture: false
  }, {
    goal: 'Find a way to<br>castle queen-side!',
    fen: '2kr2nr/pppppppp/7b/8/8/8/PPb1PPPP/RN2KR2 w KQkq -',
    nbMoves: 4,
    shapes: [arrow('e1c1')],
    success: castledQueenSide,
    failure: cantCastleQueenSide,
    detectCapture: false
  }].map(function(l, i) {
    l.autoCastle = true;
    return util.toLevel(l, i);
  }),
  complete: 'Congratulations! You should almost always castle in a game.'
};
