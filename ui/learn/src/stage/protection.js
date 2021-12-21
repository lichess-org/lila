var util = require('../util');
var arrow = util.arrow,
  circle = util.circle;

var imgUrl = util.assetUrl + 'images/learn/bolt-shield.svg';

module.exports = {
  key: 'protection',
  title: 'protection',
  subtitle: 'keepYourPiecesSafe',
  image: imgUrl,
  intro: 'protectionIntro',
  illustration: util.roundSvg(imgUrl),
  levels: [
    {
      goal: 'escape',
      fen: '9/1r7/9/9/9/2PP5/Pp7/1B7/LNS6 b - 1',
      shapes: [arrow('b3b2', 'red'), circle('b2')],
    },
    {
      // escape
      goal: 'escape',
      fen: '9/9/9/9/7Pb/9/8P/6+p2/3S3RL b - 1',
    },
    {
      // protect
      goal: 'noEscape',
      fen: '9/9/2n6/2r6/4P4/2S6/1PBP5/2R6/9 b - 1',
      shapes: [arrow('c6c4', 'red'), arrow('c2c3', 'green')],
      scenario: [
        {
          move: ['c3a5', 'c3b4', 'c3d4', 'c3a1', 'c3b2', 'c3d2', 'c3e1'],
          shapes: [arrow('c2c4', 'green')],
        },
      ],
    },
    {
      goal: 'makeSureAllSafe',
      fen: '9/9/6b2/7R1/9/9/1P7/1S7/9 b - 1',
    },
    {
      goal: 'makeSureAllSafe',
      fen: '9/9/6n2/9/9/5Pp2/6N2/9/9 b - 1',
      shapes: [arrow('g4g3', 'red')],
      scenario: [
        {
          move: 'g3f5',
          shapes: [arrow('g7f5', 'red'), arrow('f4f5', 'green')],
        },
      ],
    },
    {
      goal: 'makeSureAllSafe',
      fen: '9/9/7+P1/6s2/5N3/9/9/9/9 b - 1',
    },
    {
      goal: 'makeSureAllSafe',
      fen: '9/9/9/9/9/9/5G3/9/3+bS4 b - 1',
    },
    {
      goal: 'makeSureAllSafe',
      fen: '9/9/9/9/3n5/9/2PPS4/3R5/9 b - 1',
    },
    {
      goal: 'dontForgetYouCanDropToDefend',
      fen: '9/9/4S4/9/4r4/9/4G4/9/9 b L 1',
    },
    {
      goal: 'dontForgetYouCanDropToDefend',
      fen: '9/9/9/9/9/3B5/7S1/9/3G3r1 b NLP 1',
    },
    {
      goal: 'dontLetThemTakeAnyUndefendedPiece',
      fen: '9/9/9/9/9/9/4P4/4G4/2S2+r3 b NLP 1',
    },
    {
      goal: 'dontLetThemTakeAnyUndefendedPiece',
      fen: '9/9/9/9/7S1/7+b1/9/5G3/9 b NLP 1',
    },
    {
      goal: 'dontLetThemTakeAnyUndefendedPiece',
      fen: '8l/9/9/7n1/8P/8L/6PP1/6S2/7N1 b LP 1',
    },
  ].map(function (l, i) {
    l.nbMoves = 1;
    l.detectCapture = 'unprotected';
    return util.toLevel(l, i);
  }),
  complete: 'protectionComplete',
};
