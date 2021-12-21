var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;
var circle = util.circle;

var imgUrl = util.assetUrl + 'images/learn/crossed-swords.svg';

module.exports = {
  key: 'attack',
  title: 'attackTitle',
  subtitle: 'attackSubtitle',
  image: imgUrl,
  intro: 'attackIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      goal: 'pawnsAreDisposable',
      fen: 'l4g1kl/1r2g1sb1/2npspnp1/p1p1p1p1p/1p7/2P2PPPP/PPB1PG3/3R2SK1/LNS2G1NL b P 1',
      shapes: [circle('g7', 'red')],
      scenario: [
        {
          move: 'g4g5',
        },
        {
          move: 'g6g5',
        },
        {
          move: 'p*g6',
        },
        {
          move: 'f7f6',
        },
        {
          move: 'g6g7/',
        },
        {
          move: 'g8g7',
        },
      ],
      nbMoves: 3,
    },
  ].map(function (l, i) {
    l.success = assert.scenarioComplete;
    l.failure = assert.scenarioFailed;
    return util.toLevel(l, i);
  }),
  complete: 'attackComplete',
};
