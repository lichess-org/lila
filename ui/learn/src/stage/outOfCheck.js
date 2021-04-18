var util = require('../util');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/guards.svg';

module.exports = {
  key: 'outOfCheck',
  title: 'outOfCheck',
  subtitle: 'defendYourKing',
  image: imgUrl,
  intro: 'outOfCheckIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [{
// Even if you stay in check you pass
    goal: 'escapeWithTheKing',
    fen: '9/9/9/9/5r3/9/9/9/5K3 b - 1',
    shapes: [arrow('f5f1', 'red'), arrow('f1e1')]
  }, {
    goal: 'escapeWithTheKing',
    fen: '9/9/1b1n5/9/9/3K2r2/9/9/9 b - 1',
  }].map(function(l, i) {
    l.detectCapture = false;
    l.offerIllegalMove = true;
    l.nbMoves = 1;
    return util.toLevel(l, i);
  }),
  complete: 'outOfCheckComplete'
};
