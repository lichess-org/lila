var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;
var circle = util.circle;

var imgUrl = util.assetUrl + 'images/learn/sprint.svg';

module.exports = {
  key: 'value',
  title: 'pieceValue',
  subtitle: 'evaluatePieceStrength',
  image: imgUrl,
  intro: 'pieceValueIntroNew',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      goal: 'pawnsAreTheLeastValuable',
      fen: '9/9/3pl4/2n1+R4/9/9/9/9/9 b - 1',
      scenario: ['e6c6'],
      nbMoves: 1,
      captures: 1,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      detectCapture: true,
      pointsForCapture: true,
    },
    {
      goal: 'knightSilverGold',
      fen: '9/9/4ns3/4S4/3g5/9/9/9/9 b - 1',
      scenario: ['e6f7+'],
      nbMoves: 1,
      captures: 1,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      detectCapture: true,
      pointsForCapture: true,
    },
    {
      goal: 'goldBishopRook',
      fen: '9/9/9/5gb2/4r+Bn2/4sbl2/4gs3/9/9 b - 1',
      scenario: ['f5f4'],
      nbMoves: 1,
      captures: 1,
      offerIllegalMove: true,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      detectCapture: true,
      pointsForCapture: true,
    },
    {
      goal: 'takeAllThePiecesStartingFromTheMost',
      fen: '9/9/9/3lg4/9/3+R1n3/2r1bp3/2s6/9 b - 1',
      nbMoves: 9,
      captures: 7,
      offerIllegalMove: true,
      success: assert.extinct('white'),
      detectCapture: true,
      capturePiecesInOrderOfValue: true,
      pointsForCapture: true,
    },
    {
      goal: 'anExchangeIs',
      fen: '9/6k2/3p2g2/3s5/7N1/9/9/3R5/9 b - 1',
      scenario: [['h5g7+', 'h5g7'], 'g8g7'],
      nbMoves: 1,
      captures: 1,
      offerIllegalMove: true,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    {
      goal: 'theOpponentJustGaveAway',
      fen: '7k1/9/6+P+P+P/9/P3n4/2N6/2P6/2K+r5/L8 b 2g 1',
      shapes: [arrow('d8d2', 'red')],
      scenario: [
        {
          move: 'c2b3',
          wrongMoves: [
            ['c2d2', 'g*d3', 'd2c1', 'g*c2'],
            ['c2b1', 'g*c1'],
          ],
        },
        'g*b5',
      ],
      offerIllegalMove: true,
      nbMoves: 1,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    {
      goal: 'yourKingsValueIsInfinite',
      fen: '7k1/9/6+P+P+P/9/Pg2n4/2N6/1KP6/3+r5/L8 b g 1',
      offerIllegalMove: true,
      scenario: [
        {
          move: ['g7h8', 'h7h8', 'i7h8'],
          wrongMoves: [['g7g8'], ['h7g8'], ['h7i8'], ['i7i8'], ['any', 'g*b4']],
        },
      ],
      anyOtherMove: 'g*b4',
      nbMoves: 1,
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    /*
    {
      goal: 'takeThePieceWithTheHighestValue',
      fen: 'ln7/1skgg4/ppppp4/2P6/1N7/9/6B2/2R6/9 b - 1',
      nbMoves: 3,
      scenario: [
        {
          move: 'c6c7+',
        },
        {
          move: 'b9c7',
        },
        {
          move: 'b5c7+',
        },
        {
          move: 'b8c7',
        },
        {
          move: 'g3c7+',
        },
        {
          move: 'd8c7',
        },
      ],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    */
    {
      goal: 'twoGeneralsAreBetter',
      fen: 'l6rl/2k3g2/2ppps3/pp3pp2/2P4pp/P1BP1P3/1P2P1PPP/2S4R1/L1K2G2L b b 1',
      nbMoves: 3,
      captures: 2,
      scenario: [
        'c4f7+',
        'g8f7',
        {
          move: 's*g8',
          wrongShapes: [circle('g8')],
        },
        'h9h8',
        'g8f7+',
      ],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    {
      goal: 'rememberWhichPieceIsTheMostValuable',
      fen: 'ln+R5l/3r1gks1/5pnp1/2b3p1p/1p7/P3pP2P/1P1+b1GPP1/6SK1/5+p1NL b GSPgsnl3p 1',
      nbMoves: 1,
      scenario: [
        {
          move: 's*h9',
          shapes: [circle('g8', 'green')],
          wrongMoves: [
            ['c9d8', 's*g1', 'h2i2', 'l*i3', 'i2i3', 'g7h5', 'i3h4', 'g*g5'],
            ['s*f9', 'f8f9'],
            ['g*f9', 'f8f9'],
            ['g*g9', 'h8g9'],
            ['c9f9', 'f8f9'],
            ['c9g9', 'h8g9'],
            ['c9h9', 'g8h9'],
            ['any', 'l*f9'],
          ],
        },
      ],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
      shapes: [arrow('c9c6'), arrow('c9d8'), arrow('c9b9'), circle('c6'), circle('d8'), circle('b9')],
      detectCapture: 'unprotected',
    },
  ].map(function (l, i) {
    return util.toLevel(l, i);
  }),
  complete: 'rememberTheKingIsMoreValuableThanEveryOtherPieceCombined',
};
