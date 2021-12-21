var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/guards.svg';

module.exports = {
  key: 'outOfCheck',
  title: 'outOfCheck',
  subtitle: 'defendYourKing',
  image: imgUrl,
  intro: 'outOfCheckIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      goal: 'ifYourKingIsAttacked',
      fen: '9/9/9/9/4r4/9/9/9/4K4 b - 1',
      shapes: [arrow('e5e1', 'red')],
    },
    {
      goal: 'escapeWithTheKing',
      fen: '9/9/9/9/9/9/9/2s1r4/3K5 b - 1',
    },
    {
      goal: 'theKingCannotEscapeButBlock',
      fen: '9/9/9/9/9/9/6PPP/4+r2K1/6SNL b - 1',
    },
    {
      goal: 'theKingCannotEscapeButBlock',
      fen: '9/9/9/9/9/9/6PPP/5+r1K1/7NL b G 1',
    },
    {
      goal: 'theKingCannotEscapeButBlock',
      fen: '9/9/9/9/5b3/9/6B1P/8K/6sNL b P 1',
    },
    // 6
    {
      goal: 'youCanGetOutOfCheckByTaking',
      fen: '9/9/9/9/9/9/6PPp/5+p1KP/5GbNL b - 1',
    },
    {
      goal: 'getOutOfCheck',
      fen: '9/9/9/9/9/9/3Ss4/3s5/3K5 b - 1',
    },
    {
      goal: 'getOutOfCheck',
      fen: '9/9/9/7p1/8b/4sPK2/6g2/9/9 b - 1',
    },
    {
      goal: 'getOutOfCheck',
      fen: '9/9/9/6n2/6p2/5Ks2/9/4r4/9 b - 1',
    },
    {
      goal: 'getOutOfCheck',
      fen: '9/9/9/9/8P/8l/7P1/6rSK/7NL b P 1',
    },
    {
      goal: 'watchOutForYourOpponentsReply',
      fen: '9/9/9/9/9/4b4/PPG6/LS7/K4r3 b L 1',
    },
    {
      goal: 'watchOutForYourOpponentsReply',
      fen: '9/9/9/9/9/5+bn2/5GPPP/6SK1/5G1NL b g 1',
      scenario: [
        {
          move: 'f3g4',
          wrongMoves: [
            ['h2i2', 'g*h2'],
            ['h2g1', 'g*h2'],
          ],
        },
      ],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
  ].map(function (l, i) {
    l.detectCapture = 'unprotected';
    l.offerIllegalMove = true;
    l.nbMoves = 1;
    return util.toLevel(l, i);
  }),
  complete: 'outOfCheckComplete',
};
