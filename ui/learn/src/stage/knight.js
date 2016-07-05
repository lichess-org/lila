var m = require('mithril');
var util = require('../util');
var arrow = util.arrow;

module.exports = {
  key: 'knight',
  title: 'The knight',
  subtitle: 'It moves in a L shape',
  image: util.assetUrl + 'images/learn/pieces/N.svg',
  intro: "Here's a challenge for you. The knight is... a tricky piece.",
  illustration: util.pieceImg('knight'),
  levels: [{
    goal: 'Knights have a fancy way<br>of jumping around!',
    fen: '8/8/8/8/4N3/8/8/8 w - -',
    apples: 'c5 d7',
    nbMoves: 2,
    shapes: [arrow('e4c5'), arrow('c5d7')]
  }, {
    goal: 'Grab all the stars!',
    fen: '8/8/8/8/8/8/8/1N6 w - -',
    apples: 'c3 d4 e2 f3 f7 g5 h8',
    nbMoves: 8
  }, {
    goal: 'Grab all the stars!',
    fen: '8/2N5/8/8/8/8/8/8 w - -',
    apples: 'b6 d5 d7 e6 f4',
    nbMoves: 5
  }, {
    goal: 'Knights can jump over obstacles!<br>Escape and vanquish the stars!',
    fen: '8/8/8/8/5N2/8/8/8 w - -',
    apples: 'e3 e4 e5 f3 f5 g3 g4 g5',
    nbMoves: 9
  }, {
    goal: 'Grab all the stars!',
    fen: '8/8/8/8/8/3N4/8/8 w - -',
    apples: 'c3 e2 e4 f2 f4 g6',
    nbMoves: 6
  }, {
    goal: 'Grab all the stars!',
    fen: '8/2N5/8/8/8/8/8/8 2 - -',
    apples: 'b4 b5 c6 c8 d4 d5 e3 e7 f5',
    nbMoves: 9
  }].map(util.toLevel),
  complete: 'Congratulations! You have mastered the knight.'
};
