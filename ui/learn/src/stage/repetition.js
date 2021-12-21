var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/repetition.svg';

module.exports = {
  key: 'repetition',
  title: 'repetition',
  subtitle: 'fourfoldRepetitionIsADrawExcept',
  image: imgUrl,
  intro: 'repetitionIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      goal: 'ifTheSamePositionOccurs',
      fen: '9/9/9/9/k8/9/+p8/3+p5/K8 b - 1',
      shapes: [arrow('c6e8', 'green'), arrow('e8e3', 'green'), arrow('c3g3', 'green')],
      scenario: [
        'a1b1',
        'a5b5',
        'b1a1',
        {
          move: 'b5a5',
          shapes: [
            arrow('c7d8', 'green'),
            arrow('d8f8', 'green'),
            arrow('f8g7', 'green'),
            arrow('g7c3', 'green'),
            arrow('c3g3', 'green'),
          ],
        },
        'a1b1',
        'a5b5',
        'b1a1',
        {
          move: 'b5a5',
          shapes: [
            arrow('c7d8', 'green'),
            arrow('d8f8', 'green'),
            arrow('f8g7', 'green'),
            arrow('g7e6', 'green'),
            arrow('e6e5', 'green'),
            arrow('e5g4', 'green'),
            arrow('g4f3', 'green'),
            arrow('f3d3', 'green'),
            arrow('d3c4', 'green'),
          ],
        },
        'a1b1',
        'a5b5',
        'b1a1',
        {
          move: 'b5a5',
          shapes: [arrow('f3f8', 'green'), arrow('f8c5', 'green'), arrow('c5g5', 'green')],
        },
      ],
      nbMoves: 6,
      captures: 0,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      detectCapture: true,
    },
    {
      goal: 'perpetualCheckIsALoss',
      fen: '3k5/9/1s+P+P+P4/9/P2+b5/KP7/1+b7/9/9 b -',
      scenario: ['a4b5', 'b3c4', 'b5a4', 'c4b3', 'a4b5', 'b3c4', 'b5a4', 'c4b3', 'a4b5', 'b3c4', 'b5a4', 'c4b3'],
      nbMoves: 6,
      captures: 0,
      offerIllegalMove: true,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    {
      goal: 'boardFlippedFindBestMove',
      fen: '3k5/9/1s+P+P+P4/9/P2+b5/KP+b6/9/9/9 w -',
      scenario: ['c4b3', 'a4b5', 'd5c4'],
      nbMoves: 2,
      offerIllegalMove: true,
      showFailureFollowUp: true,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
  ].map(function (l, i) {
    return util.toLevel(l, i);
  }),
  complete: 'repetitionComplete',
};
