var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow,
  circle = util.circle;

var imgUrl = util.assetUrl + 'images/learn/scales.svg';

module.exports = {
  key: 'stalemate',
  title: 'stalemate',
  subtitle: 'theGameIsADraw',
  image: imgUrl,
  intro: 'stalemateIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      goal: 'stalemateGoal',
      fen: 'k7/8/8/6B1/8/1R6/8/8 w - -',
      shapes: [arrow('g5e3')],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      scenario: [
        {
          move: 'g5e3',
          shapes: [
            arrow('e3a7', 'blue'),
            arrow('b3b7', 'blue'),
            arrow('b3b8', 'blue'),
            circle('a7', 'blue'),
            circle('b7', 'blue'),
            circle('b8', 'blue'),
          ],
        },
      ],
      nextButton: true,
      showFailureFollowUp: true,
    },
    {
      goal: 'stalemateGoal',
      fen: '8/7p/4N2k/8/8/3N4/8/1K6 w - -',
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      scenario: [
        {
          move: 'd3f4',
          shapes: [
            arrow('e6g7', 'blue'),
            arrow('e6g5', 'blue'),
            arrow('f4g6', 'blue'),
            arrow('f4h5', 'blue'),
            circle('g7', 'blue'),
            circle('g5', 'blue'),
            circle('g6', 'blue'),
            circle('h5', 'blue'),
          ],
        },
      ],
      nextButton: true,
      showFailureFollowUp: true,
    },
    {
      goal: 'stalemateGoal',
      fen: '4k3/6p1/5p2/p4P2/PpB2N2/1K6/8/3R4 w - -',
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      scenario: [
        {
          move: 'f4g6',
          shapes: [arrow('c4f7', 'blue'), arrow('d1d8', 'blue'), arrow('g6e7', 'blue'), arrow('g6f8', 'blue')],
        },
      ],
      nextButton: true,
      showFailureFollowUp: true,
    },
    {
      goal: 'stalemateGoal',
      fen: '8/6pk/6np/7K/8/3B4/8/1R6 w - -',
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      scenario: [
        {
          move: 'b1b8',
          shapes: [arrow('b8g8', 'blue'), arrow('b8h8', 'blue'), arrow('d3h7', 'red'), arrow('g6e7', 'red')],
        },
      ],
      nextButton: true,
      showFailureFollowUp: true,
    },
    {
      goal: 'stalemateGoal',
      fen: '7R/pk6/p1pP4/K7/3BB2p/7p/1r5P/8 w - -',
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      scenario: [
        {
          move: 'd4b2',
          shapes: [
            arrow('h8a8', 'blue'),
            arrow('a5b6', 'blue'),
            arrow('d6c7', 'blue'),
            arrow('e4b7', 'red'),
            arrow('c6c5', 'red'),
          ],
        },
      ],
      nextButton: true,
      showFailureFollowUp: true,
    },
  ].map(function (l, i) {
    l.detectCapture = false;
    l.nbMoves = 1;
    return util.toLevel(l, i);
  }),
  complete: 'stalemateComplete',
};
