var util = require('../util');
var assert = require('../assert');
var and = assert.and;
var arrow = util.arrow,
  circle = util.circle;

var imgUrl = util.assetUrl + 'images/learn/rally-the-troops.svg';

module.exports = {
  key: 'setup',
  title: 'boardSetup',
  subtitle: 'howTheGameStarts',
  image: imgUrl,
  intro: 'boardSetupIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      goal: 'thisIsTheInitialPosition',
      fen: 'lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1',
      nbMoves: 1,
    },
    {
      goal: 'firstPlaceTheKing',
      fen: '4k4/9/9/9/9/9/9/6K2/9 b RB2G2S2N2L9Prb2g2s2n2l9p 1',
      nbMoves: 2,
      shapes: [circle('e1')],
      success: assert.pieceOn('K', 'e1'),
    },
    {
      goal: 'dropAGold',
      fen: '4k4/9/9/9/9/9/9/9/4K4 b RB2G2S2N2L9Prb2g2s2n2l9p 1',
      nbMoves: 2,
      shapes: [circle('d1')],
      scenario: [
        'g*d1',
        {
          move: 'g*f9',
          shapes: [circle('f1', 'green')],
        },
        'g*f1',
        'g*d9',
      ],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    {
      goal: 'thenPlaceASilver',
      fen: '3gkg3/9/9/9/9/9/9/9/3GKG3 b RB2S2N2L9Prb2s2n2l9p 1',
      nbMoves: 2,
      shapes: [circle('c1', 'green')],
      scenario: [
        's*c1',
        {
          move: 's*g9',
          shapes: [circle('g1', 'green')],
        },
        's*g1',
        's*c9',
      ],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    {
      goal: 'dropTheKnights',
      fen: '2sgkgs2/9/9/9/9/9/9/9/2SGKGS2 b RB2N2L9Prb2n2l9p 1',
      nbMoves: 2,
      scenario: ['n*b1', 'n*h9', 'n*h1', 'n*b9'],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    {
      goal: 'dropTheLances',
      fen: '1nsgkgsn1/9/9/9/9/9/9/9/1NSGKGSN1 b RB2L9Prb2l9p 1',
      nbMoves: 2,
      scenario: ['l*a1', 'p*i7', 'l*i1', 'p*h7'],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    {
      goal: 'placeTheBishopThenRook',
      fen: '1nsgkgsn1/9/7pp/9/9/9/9/9/LNSGKGSNL b RB9Prb2l7p 1',
      nbMoves: 2,
      scenario: ['b*b2', 'p*g7', 'r*h2', 'p*f7'],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    {
      goal: 'placeThePawns',
      fen: '1nsgkgsn1/9/5pppp/9/9/9/9/1B5R1/LNSGKGSNL b 9Prb2l5p 1',
      nbMoves: 9,
      shapes: [circle('e3', 'green')],
      scenario: [
        'p*e3',
        {
          move: 'p*e7',
          shapes: [circle('d3', 'green')],
          delay: 500,
        },
        'p*d3',
        {
          move: 'p*d7',
          shapes: [circle('f3', 'green')],
          delay: 200,
        },
        'p*f3',
        {
          move: 'p*c7',
          shapes: [circle('c3', 'green')],
          delay: 200,
        },
        'p*c3',
        {
          move: 'p*b7',
          shapes: [circle('g3', 'green')],
          delay: 200,
        },
        'p*g3',
        {
          move: 'p*a7',
          delay: 100,
        },
        'p*b3',
        {
          move: 'l*i9',
          delay: 100,
        },
        'p*h3',
        {
          move: 'l*a9',
          delay: 100,
        },
        'p*a3',
        {
          move: 'b*h8',
          delay: 100,
        },
        'p*i3',
        {
          move: 'r*b8',
          delay: 100,
        },
      ],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
    {
      goal: 'pushingThe3rdPawn',
      fen: 'lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1',
      nbMoves: 2,
      shapes: [arrow('c3c4', 'green')],
      scenario: [
        {
          move: 'c3c4',
          shapes: [arrow('b2g7', 'green')],
        },
      ],
      success: assert.scenarioComplete,
      failure: assert.scenarioFailed,
    },
  ].map(function (l, i) {
    return util.toLevel(l, i);
  }),
  complete: 'boardSetupComplete',
  cssClass: 'no-go-home',
};
