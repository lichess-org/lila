var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/winged-sword.svg';

module.exports = {
  key: 'check1',
  title: 'checkInOne',
  subtitle: 'attackTheOpponentsKing',
  image: imgUrl,
  intro: 'checkInOneIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      goal: 'checkInOneGoal',
      fen: '5R3/4Ssk2/5p3/9/7L1/9/9/9/9 b r2b4g2s4n3l17p 3',
      shapes: [arrow('f9h9')],
    },
    {
      goal: 'checkInOneGoal',
      fen: '3g5/2rk1g3/pps2pppp/2S6/9/9/9/9/9 b G 1',
    },
    {
      goal: 'checkInOneGoal',
      fen: '9/9/9/2B2r3/9/4L4/9/9/7k1 b - 1',
    },
    {
      goal: 'checkInOneGoal',
      fen: '5+B2s/7k1/7N1/6Ss1/9/9/9/9/9 b r2b4g2s4n3l17p 1',
    },
    {
      goal: 'checkInOneGoal',
      fen: '6snl/4+Rgk2/5pppp/9/9/9/9/9/9 b GSr2b2g2s3n3l14p 1',
    },
  ].map(function (l, i) {
    l.nbMoves = 1;
    l.failure = assert.not(assert.check);
    l.success = assert.check;
    l.detectCapture = 'unprotected';
    return util.toLevel(l, i);
  }),
  complete: 'checkInOneComplete',
};
