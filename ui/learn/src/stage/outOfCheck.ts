import { arrow, assetUrl, roundSvg, toLevel } from '../util';
import type { StageNoID } from './list';

const imgUrl = assetUrl + 'images/learn/guards.svg';

const common = {
  detectCapture: false,
  offerIllegalMove: true,
  nbMoves: 1,
};

const stage: StageNoID = {
  key: 'outOfCheck',
  title: i18n.learn.outOfCheck,
  subtitle: i18n.learn.defendYourKing,
  image: imgUrl,
  intro: i18n.learn.outOfCheckIntro,
  illustration: roundSvg(imgUrl),
  levels: [
    {
      goal: i18n.learn.escapeWithTheKing,
      fen: '8/8/8/4q3/8/8/8/4K3 w - -',
      shapes: [arrow('e5e1', 'red'), arrow('e1f1')],
    },
    {
      goal: i18n.learn.escapeWithTheKing,
      fen: '8/2n5/5b2/8/2K5/8/2q5/8 w - -',
    },
    {
      goal: i18n.learn.theKingCannotEscapeButBlock,
      fen: '8/7r/6r1/8/R7/7K/8/8 w - -',
    },
    {
      goal: i18n.learn.youCanGetOutOfCheckByTaking,
      fen: '8/8/8/3b4/8/4N3/KBn5/1R6 w - -',
    },
    {
      goal: i18n.learn.thisKnightIsCheckingThroughYourDefenses,
      fen: '4q3/8/8/8/8/5nb1/3PPP2/3QKBNr w - -',
    },
    {
      goal: i18n.learn.escapeOrBlock,
      fen: '8/8/7p/2q5/5n2/1N1KP2r/3R4/8 w - -',
    },
    {
      goal: i18n.learn.escapeOrBlock,
      fen: '8/6b1/8/8/q4P2/2KN4/3P4/8 w - -',
    },
  ].map((l, i) => toLevel({ ...common, ...l }, i)),
  complete: i18n.learn.outOfCheckComplete,
};
export default stage;
