import { arrow, assetUrl, roundSvg, toLevel } from '../util';
import { mate, not } from '../assert';

const imgUrl = assetUrl + 'images/learn/guillotine.svg';

const common = () => ({
  nbMoves: 1,
  failure: not(mate),
  success: mate,
  showFailureFollowUp: true,
});

export default {
  key: 'checkmate1',
  title: 'mateInOne',
  subtitle: 'defeatTheOpponentsKing',
  image: imgUrl,
  intro: 'mateInOneIntro',
  illustration: roundSvg(imgUrl),
  levels: [
    {
      // rook
      goal: 'attackYourOpponentsKing',
      fen: '3qk3/3ppp2/8/8/2B5/5Q2/8/8 w - -',
      shapes: [arrow('f3f7')],
    },
    {
      // smothered
      goal: 'attackYourOpponentsKing',
      fen: '6rk/6pp/7P/6N1/8/8/8/8 w - -',
    },
    {
      // rook
      goal: 'attackYourOpponentsKing',
      fen: 'R7/8/7k/2r5/5n2/8/6Q1/8 w - -',
    },
    {
      // Q+N
      goal: 'attackYourOpponentsKing',
      fen: '2rb4/2k5/5N2/1Q6/8/8/8/8 w - -',
    },
    {
      // discovered
      goal: 'attackYourOpponentsKing',
      fen: '1r2kb2/ppB1p3/2P2p2/2p1N3/B7/8/8/3R4 w - -',
    },
    {
      // tricky
      goal: 'attackYourOpponentsKing',
      fen: '8/pk1N4/n7/b7/6B1/1r3b2/8/1RR5 w - -',
      scenario: [
        {
          move: 'g4f3',
          shapes: [arrow('b1b7', 'yellow'), arrow('f3b7', 'yellow')],
        },
      ],
    },
    {
      // tricky
      goal: 'attackYourOpponentsKing',
      fen: 'r1b5/ppp5/2N2kpN/5q2/8/Q7/8/4B3 w - -',
    },
  ].map((l, i) => toLevel({ ...common(), ...l }, i)),
  complete: 'mateInOneComplete',
};
