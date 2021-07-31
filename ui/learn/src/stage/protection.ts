const util = require('../util');
const arrow = util.arrow;

const imgUrl = util.assetUrl + 'images/learn/bolt-shield.svg';

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
      fen: '8/8/8/4bb2/8/8/P2P4/R2K4 w - -',
      shapes: [arrow('e5a1', 'red'), arrow('a1c1')],
    },
    {
      // escape
      goal: 'escape',
      fen: '8/8/2q2N2/8/8/8/8/8 w - -',
    },
    {
      // protect
      goal: 'noEscape',
      fen: '8/N2q4/8/8/8/8/6R1/8 w - -',
      scenario: [
        {
          move: 'g2a2',
          shapes: [arrow('a2a7', 'green')],
        },
      ],
    },
    {
      goal: 'noEscape',
      fen: '8/8/1Bq5/8/2P5/8/8/8 w - -',
    },
    {
      goal: 'noEscape',
      fen: '1r6/8/5b2/8/8/5N2/P2P4/R1B5 w - -',
      shapes: [arrow('f6a1', 'red'), arrow('d2d4')],
    },
    {
      goal: 'dontLetThemTakeAnyUndefendedPiece',
      fen: '8/1b6/8/8/8/3P2P1/5NRP/r7 w - -',
    },
    {
      goal: 'dontLetThemTakeAnyUndefendedPiece',
      fen: 'rr6/3q4/4n3/4P1B1/7P/P7/1B1N1PP1/R5K1 w - -',
    },
    {
      goal: 'dontLetThemTakeAnyUndefendedPiece',
      fen: '8/3q4/8/1N3R2/8/2PB4/8/8 w - -',
    },
  ].map(function (l, i) {
    l.nbMoves = 1;
    return util.toLevel(l, i);
  }),
  complete: 'protectionComplete',
};
