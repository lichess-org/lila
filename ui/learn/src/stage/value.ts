import { arrow, assetUrl, roundSvg, toLevel } from '../util';
import { scenarioComplete, scenarioFailed } from '../assert';
import type { StageNoID } from './list';

const imgUrl = assetUrl + 'images/learn/sprint.svg';

const common = {
  nbMoves: 1,
  captures: 1,
  pointsForCapture: true,
  showPieceValues: true,
};

const stage: StageNoID = {
  key: 'value',
  title: i18n.learn.pieceValue,
  subtitle: i18n.learn.evaluatePieceStrength,
  image: imgUrl,
  intro: i18n.learn.pieceValueIntro,
  illustration: roundSvg(imgUrl),
  levels: [
    {
      // rook
      goal: i18n.learn.queenOverBishop,
      fen: '8/8/2qrbnp1/3P4/8/8/8/8 w - -',
      scenario: ['d5c6'],
      shapes: [arrow('d5c6')],
      success: scenarioComplete,
      failure: scenarioFailed,
      detectCapture: false,
    },
    {
      goal: i18n.learn.pieceValueExchange,
      fen: '8/8/4b3/1p6/6r1/8/4Q3/8 w - -',
      scenario: ['e2e6'],
      success: scenarioComplete,
      failure: scenarioFailed,
      detectCapture: true,
    },
    {
      goal: i18n.learn.pieceValueLegal,
      fen: '5b2/8/6N1/2q5/3Kn3/2rp4/3B4/8 w - -',
      scenario: ['d4e4'],
      offerIllegalMove: true,
      success: scenarioComplete,
      failure: scenarioFailed,
    },
    {
      goal: i18n.learn.takeThePieceWithTheHighestValue,
      fen: '1k4q1/pp6/8/3B4/2P5/1P1p2P1/P3Kr1P/3n4 w - -',
      scenario: ['e2d1'],
      offerIllegalMove: true,
      success: scenarioComplete,
      failure: scenarioFailed,
      detectCapture: false,
    },
    {
      goal: i18n.learn.takeThePieceWithTheHighestValue,
      fen: '7k/3bqp1p/7r/5N2/6K1/6n1/PPP5/R1B5 w - -',
      scenario: ['c1h6'],
      offerIllegalMove: true,
      success: scenarioComplete,
      failure: scenarioFailed,
    },
  ].map((l, i) => toLevel({ ...common, ...l }, i)),
  complete: i18n.learn.pieceValueComplete,
};
export default stage;
