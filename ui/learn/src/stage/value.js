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
  levels: [{
    goal: 'takeThePieceWithTheHighestValue',
    fen: '9/9/9/1r7/6r2/9/4B4/9/9 b - 1',
    scenario: ['e3b6'],
    nbMoves: 1,
    captures: 1,
    success: assert.scenarioComplete,
    failure: assert.scenarioFailed,
    detectCapture: true
  }, {
    goal: 'takeThePieceWithTheHighestValue',
    fen: '9/9/9/1r7/6r2/9/4B4/9/9 b - 1',
    scenario: ['d4e4'],
    nbMoves: 1,
    captures: 1,
    offerIllegalMove: true,
    success: assert.scenarioComplete,
    failure: assert.scenarioFailed
  }, {
    goal: 'takeThePieceWithTheHighestValue',
    fen: '1k4q1/pp6/8/3B4/2P5/1P1p2P1/P3Kr1P/3n4 b -',
    scenario: ['e2d1'],
    nbMoves: 1,
    captures: 1,
    offerIllegalMove: true,
    success: assert.scenarioComplete,
    failure: assert.scenarioFailed,
    detectCapture: false
  }, {
    goal: 'takeThePieceWithTheHighestValue',
    fen: '7k/3bqp1p/7r/5N2/6K1/6n1/PPP5/R1B5 b -',
    scenario: ['c1h6'],
    nbMoves: 1,
    captures: 1,
    offerIllegalMove: true,
    success: assert.scenarioComplete,
    failure: assert.scenarioFailed
  }].map(function(l, i) {
    l.pointsForCapture = true;
    l.showPieceValues = true;
    return util.toLevel(l, i);
  }),
  complete: 'pieceValueComplete'
};
