import { anyCapture, extinct } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, circle, concat, initial, onDest } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: 'capturedPiecesCanBeDropped',
    sfen: '9/9/8r/4n4/9/9/4L4/9/9 b -',
    nbMoves: 3,
    success: extinct('gote'),
    failure: anyCapture,
    drawShapes: onDest('5d', [arrow(toPiece('N'), '2e')]),
    showFailureMove: 'capture',
  },
  {
    goal: 'dropLimitations',
    sfen: '9/9/9/9/3nbl3/9/9/9/9 b P',
    nbMoves: 6,
    success: extinct('gote'),
    failure: anyCapture,
    drawShapes: concat(
      initial([
        circle('9a', 'red'),
        circle('8a', 'red'),
        circle('7a', 'red'),
        circle('6a', 'red'),
        circle('5a', 'red'),
        circle('4a', 'red'),
        circle('3a', 'red'),
        circle('2a', 'red'),
        circle('1a', 'red'),
        circle('4e', 'red'),
        circle('5e', 'red'),
        circle('6e', 'red'),
        arrow(toPiece('P'), '5f'),
      ])
    ),
    showFailureMove: 'capture',
  },
  {
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/9/9/2g1s4/9/9/9/6N2/9 b - 1',
    nbMoves: 4,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'capture',
  },
  {
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/5p3/4Gl3/9/9/5g3/9/9/9 b - 1',
    nbMoves: 4,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'capture',
  },
  {
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '4n4/s3g4/8g/9/4B4/9/9/9/9 b - 1',
    nbMoves: 7,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'capture',
  },
  {
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/3rn4/5S3/3lp4/9/7b1/9/9/9 b - 1',
    nbMoves: 7,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'capture',
  },
  {
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/9/2n1r1b2/9/3s1g1R1/9/9/9/9 b - 1',
    nbMoves: 8,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'capture',
  },
  {
    goal: 'youCannotHaveTwoUnpromotedPawns',
    sfen: '9/9/4s4/9/9/9/2PP1PP2/9/9 b P 1',
    nbMoves: 4,
    success: extinct('gote'),
  },
  {
    goal: 'youCannotHaveTwoUnpromotedPawns',
    sfen: '9/4G4/1n2p2b1/9/9/1+P5P1/9/9/9 b - 1',
    nbMoves: 5,
    success: extinct('gote'),
    drawShapes: initial([arrow('5b', '5c')]),
  },
];

const stage: IncompleteStage = {
  key: 'drop',
  title: 'pieceDrops',
  subtitle: 'reuseCapturedPieces',
  intro: 'dropIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'captureComplete',
};

export default stage;
