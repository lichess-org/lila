import { anyCapture, extinct } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, circle, initial, onDest } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    // lance
    goal: 'takeTheEnemyPieces',
    sfen: '9/4n4/9/9/4p4/9/9/4L4/9 b -',
    nbMoves: 2,
    success: extinct('gote'),
    failure: anyCapture,
    drawShapes: initial([arrow('5h', '5e'), arrow('5e', '5b')]),
    showFailureMove: 'capture',
  },
  {
    // gold
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/9/4nr3/4G4/9/9/9/9/9 b -',
    nbMoves: 2,
    success: extinct('gote'),
    failure: anyCapture,
    drawShapes: initial([arrow('4c', '5c', 'red')]),
    showFailureMove: 'capture',
  },
  {
    // bishop
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/9/9/4p4/9/4B1s2/5g3/9/9 b -',
    nbMoves: 4,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'capture',
  },
  {
    // knight
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/3spg3/3p1p3/4N4/9/9/9/9/9 b -',
    nbMoves: 5,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'capture',
  },
  {
    // rook
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/4r1s2/5n3/3R1b3/9/9/9/9/9 b -',
    nbMoves: 6,
    success: extinct('gote'),
    failure: anyCapture,
    drawShapes: onDest('4d', [circle(toPiece('B'))]),
    showFailureMove: 'capture',
  },
  {
    // silver
    goal: 'takeTheEnemyPiecesAndDontLoseYours',
    sfen: '9/5l+p2/4S4/4pp3/9/9/9/9/9 b -',
    nbMoves: 7,
    success: extinct('gote'),
    failure: anyCapture,
    showFailureMove: 'capture',
  },
];

const stage: IncompleteStage = {
  key: 'capture',
  title: 'capture',
  subtitle: 'takeTheEnemyPieces',
  intro: 'captureIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'captureComplete',
};

export default stage;
