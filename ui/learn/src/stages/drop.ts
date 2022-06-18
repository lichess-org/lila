import { anyCapture, extinct } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, initial, onDest } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    // lance
    goal: 'capturedPiecesCanBeDropped',
    sfen: '9/9/8r/4n4/9/9/4L4/9/9 b -',
    nbMoves: 3,
    success: extinct('gote'),
    failure: anyCapture,
    drawShapes: onDest('5d', [arrow(toPiece('N'), '2e')]),
    showFailureMove: 'capture',
  },
  {
    // knight
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/9/9/2g1s4/9/9/9/6N2/9 b - 1',
    nbMoves: 4,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'capture',
  },
  {
    // gold
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/5p3/4Gl3/9/9/5g3/9/9/9 b - 1',
    nbMoves: 4,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'capture',
  },
  {
    // bishop
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '4n4/g3g4/8s/9/4B4/9/9/9/9 b - 1',
    nbMoves: 7,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'capture',
  },
  {
    // silver
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/3rn4/5S3/3lp4/9/7b1/9/9/9 b - 1',
    nbMoves: 7,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'unprotected',
  },
  {
    // rook
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/9/2n1r1b2/9/3s1g1R1/9/9/9/9 b - 1',
    nbMoves: 8,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'unprotected',
  },
  {
    // nifu 1
    goal: 'youCannotHaveTwoUnpromotedPawns',
    sfen: '9/9/4s4/9/9/9/2PP1PP2/9/9 b P 1',
    nbMoves: 4,
    success: extinct('gote'),
  },
  {
    // nifu 2
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
