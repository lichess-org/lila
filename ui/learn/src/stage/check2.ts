import { checkIn, noCheckIn } from '../assert';
import { arrow, assetUrl, roundSvg, toLevel } from '../util';
import type { StageNoID } from './list';

const imgUrl = assetUrl + 'images/learn/crossed-swords.svg';

const common = () => ({
  nbMoves: 2,
  failure: noCheckIn(2),
  success: checkIn(2),
});

const stage: StageNoID = {
  key: 'check2',
  title: i18n.learn.checkInTwo,
  subtitle: i18n.learn.twoMovesToGiveCheck,
  image: imgUrl,
  intro: i18n.learn.checkInTwoIntro,
  illustration: roundSvg(imgUrl),
  levels: [
    {
      goal: i18n.learn.checkInTwoGoal,
      fen: '2k5/2pb4/8/2R5/8/8/8/8 w - -',
      shapes: [arrow('c5a5'), arrow('a5a8')],
    },
    {
      goal: i18n.learn.checkInTwoGoal,
      fen: '8/8/5k2/8/8/1N6/5b2/8 w - -',
    },
    {
      goal: i18n.learn.checkInTwoGoal,
      fen: '6k1/2r3pp/8/1N6/8/8/4B3/8 w - -',
    },
    {
      goal: i18n.learn.checkInTwoGoal,
      fen: 'r3k3/7b/8/4B3/8/8/4N3/4R3 w - -',
    },
    {
      goal: i18n.learn.checkInTwoGoal,
      fen: 'r1bqkb1r/pppp1p1p/2n2np1/4p3/2B5/4PN2/PPPP1PPP/RNBQK2R w KQkq -',
    },
    {
      goal: i18n.learn.checkInTwoGoal,
      fen: '8/8/8/2k5/q7/4N3/3B4/8 w - -',
    },
    {
      goal: i18n.learn.checkInTwoGoal,
      fen: 'r6r/1Q2nk2/1B3p2/8/8/8/8/8 w - -',
    },
  ].map((l, i) => toLevel({ ...common(), ...l }, i)),
  complete: i18n.learn.checkInTwoComplete,
};
export default stage;
