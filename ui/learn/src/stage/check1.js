var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/winged-sword.svg';

var oneMove = 'Aim at the opponent\'s king<br>in one move!';

module.exports = {
  key: 'check1',
  title: 'Check in one',
  subtitle: 'Attack the opponent\'s king',
  image: imgUrl,
  intro: 'To check your opponent, attack their king. They must defend it!',
  illustration: util.roundSvg(imgUrl),
  levels: [{
    goal: oneMove,
    fen: '4k3/8/2b5/8/8/8/8/R7 w - -',
    shapes: [arrow('a1e1')]
  }, {
    goal: oneMove,
    fen: '8/8/4k3/3n4/8/1Q6/8/8 w - -',
  }, {
    goal: oneMove,
    fen: '3qk3/1pp5/3p4/4p3/8/3B4/6r1/8 w - -',
  }, {
    goal: oneMove,
    fen: '2r2q2/2n5/8/4k3/8/2N1P3/3P2B1/8 w - -',
  }, {
    goal: oneMove,
    fen: '8/2b1q2n/1ppk4/2N5/8/8/8/8 w - -',
  }, {
    goal: oneMove,
    fen: '6R1/1k3r2/8/4Q3/8/2n5/8/8 w - -',
  }, {
    goal: oneMove,
    fen: '7r/4k3/8/3n4/4N3/8/2R5/4Q3 w - -',
  }].map(function(l, i) {
    l.nbMoves = 1;
    l.failure = assert.not(assert.check);
    l.success = assert.check;
    return util.toLevel(l, i);
  }),
  complete: 'Congratulations! You checked your opponent, forcing them to defend their king!'
};
