import { arrow, assetUrl, roundSvg, toLevel } from '../util';
import type { LevelPartial, StageNoID } from './list';
import { extinct } from '../assert';

const imgUrl = assetUrl + 'images/learn/bowman.svg';
const stage: StageNoID = {
  key: 'capture',
  title: i18n.learn.capture,
  subtitle: i18n.learn.takeTheEnemyPieces,
  image: imgUrl,
  intro: i18n.learn.captureIntro,
  illustration: roundSvg(imgUrl),
  levels: [
    {
      // rook
      goal: i18n.learn.takeTheBlackPieces,
      fen: '8/2p2p2/8/8/8/2R5/8/8 w - -',
      nbMoves: 2,
      captures: 2,
      shapes: [arrow('c3c7'), arrow('c7f7')],
    },
    {
      // queen
      goal: i18n.learn.takeTheBlackPiecesAndDontLoseYours,
      fen: '8/2r2p2/8/8/5Q2/8/8/8 w - -',
      nbMoves: 2,
      captures: 2,
      shapes: [arrow('f4c7'), arrow('f4f7', 'red'), arrow('c7f7', 'yellow')],
    },
    {
      // bishop
      goal: i18n.learn.takeTheBlackPiecesAndDontLoseYours,
      fen: '8/5r2/8/1r3p2/8/3B4/8/8 w - -',
      nbMoves: 5,
      captures: 3,
    },
    {
      // queen
      goal: i18n.learn.takeTheBlackPiecesAndDontLoseYours,
      fen: '8/5b2/5p2/3n2p1/8/6Q1/8/8 w - -',
      nbMoves: 7,
      captures: 4,
    },
    {
      // knight
      goal: i18n.learn.takeTheBlackPiecesAndDontLoseYours,
      fen: '8/3b4/2p2q2/8/3p1N2/8/8/8 w - -',
      nbMoves: 6,
      captures: 4,
    },
  ].map((l: LevelPartial, i) => toLevel({ ...l, pointsForCapture: true, success: extinct('black') }, i)),
  complete: i18n.learn.captureComplete,
};

export default stage;
