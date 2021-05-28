var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/sprint.svg';

module.exports = {
  key: 'value',
  title: 'pieceValue',
  subtitle: 'evaluatePieceStrength',
  image: imgUrl,
  intro: 'pieceValueIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      // rook
      goal: 'queenOverBishop',
      fen: '8/8/2qrbnp1/3P4/8/8/8/8 w - -',
      scenario: ['d5c6'],
      nbMoves: 1,
      captures: 1,
      shapes: [arrow('d5c6')],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      detectCapture: false,
    },
    {
      goal: 'pieceValueExchange',
      fen: '8/8/4b3/1p6/6r1/8/4Q3/8 w - -',
      scenario: ['e2e6'],
      nbMoves: 1,
      captures: 1,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      detectCapture: true,
    },
    {
      goal: 'pieceValueLegal',
      fen: '5b2/8/6N1/2q5/3Kn3/2rp4/3B4/8 w - -',
      scenario: ['d4e4'],
      nbMoves: 1,
      captures: 1,
      offerIllegalMove: true,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    {
      goal: 'takeThePieceWithTheHighestValue',
      fen: '1k4q1/pp6/8/3B4/2P5/1P1p2P1/P3Kr1P/3n4 w - -',
      scenario: ['e2d1'],
      nbMoves: 1,
      captures: 1,
      offerIllegalMove: true,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      detectCapture: false,
    },
    {
      goal: 'takeThePieceWithTheHighestValue',
      fen: '7k/3bqp1p/7r/5N2/6K1/6n1/PPP5/R1B5 w - -',
      scenario: ['c1h6'],
      nbMoves: 1,
      captures: 1,
      offerIllegalMove: true,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
  ].map(function (l, i) {
    l.pointsForCapture = true;
    l.showPieceValues = true;
    return util.toLevel(l, i);
  }),
  complete: 'pieceValueComplete',
};
