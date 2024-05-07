import { arrow, assetUrl, roundSvg, toLevel } from '../util';

const imgUrl = assetUrl + 'images/learn/guards.svg';

const common = {
  detectCapture: false,
  offerIllegalMove: true,
  nbMoves: 1,
};

export default {
  key: 'outOfCheck',
  title: 'outOfCheck',
  subtitle: 'defendYourKing',
  image: imgUrl,
  intro: 'outOfCheckIntro',
  illustration: roundSvg(imgUrl),
  levels: [
    {
      goal: 'escapeWithTheKing',
      fen: '8/8/8/4q3/8/8/8/4K3 w - -',
      shapes: [arrow('e5e1', 'red'), arrow('e1f1')],
    },
    {
      goal: 'escapeWithTheKing',
      fen: '8/2n5/5b2/8/2K5/8/2q5/8 w - -',
    },
    {
      goal: 'theKingCannotEscapeButBlock',
      fen: '8/7r/6r1/8/R7/7K/8/8 w - -',
    },
    {
      goal: 'youCanGetOutOfCheckByTaking',
      fen: '8/8/8/3b4/8/4N3/KBn5/1R6 w - -',
    },
    {
      goal: 'thisKnightIsCheckingThroughYourDefenses',
      fen: '4q3/8/8/8/8/5nb1/3PPP2/3QKBNr w - -',
    },
    {
      goal: 'escapeOrBlock',
      fen: '8/8/7p/2q5/5n2/1N1KP2r/3R4/8 w - -',
    },
    {
      goal: 'escapeOrBlock',
      fen: '8/6b1/8/8/q4P2/2KN4/3P4/8 w - -',
    },
  ].map((l, i) => toLevel({ ...common, ...l }, i)),
  complete: 'outOfCheckComplete',
};
