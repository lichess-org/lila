import { check, not } from '../assert';
import { arrow, assetUrl, roundSvg, toLevel } from '../util';

const imgUrl = assetUrl + 'images/learn/winged-sword.svg';

const common = () => ({
  nbMoves: 1,
  failure: not(check),
  success: check,
});

export default {
  key: 'check1',
  title: 'checkInOne',
  subtitle: 'attackTheOpponentsKing',
  image: imgUrl,
  intro: 'checkInOneIntro',
  illustration: roundSvg(imgUrl),
  levels: [
    {
      goal: 'checkInOneGoal',
      fen: '4k3/8/2b5/8/8/8/8/R7 w - -',
      shapes: [arrow('a1e1')],
    },
    {
      goal: 'checkInOneGoal',
      fen: '8/8/4k3/3n4/8/1Q6/8/8 w - -',
    },
    {
      goal: 'checkInOneGoal',
      fen: '3qk3/1pp5/3p4/4p3/8/3B4/6r1/8 w - -',
    },
    {
      goal: 'checkInOneGoal',
      fen: '2r2q2/2n5/8/4k3/8/2N1P3/3P2B1/8 w - -',
    },
    {
      goal: 'checkInOneGoal',
      fen: '8/2b1q2n/1ppk4/2N5/8/8/8/8 w - -',
    },
    {
      goal: 'checkInOneGoal',
      fen: '6R1/1k3r2/8/4Q3/8/2n5/8/8 w - -',
    },
    {
      goal: 'checkInOneGoal',
      fen: '7r/4k3/8/3n4/4N3/8/2R5/4Q3 w - -',
    },
  ].map((l, i) => toLevel({ ...common(), ...l }, i)),
  complete: 'checkInOneComplete',
};
