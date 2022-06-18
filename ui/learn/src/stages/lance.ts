import { obstaclesCaptured } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, initial, pieceMovesHighlihts } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: 'lancesAreStraighforward',
    sfen: '9/9/9/9/9/9/9/9/4L4 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5g', '5d'],
    drawShapes: initial([arrow('5i', '5g'), arrow('5g', '5d')]),
  },
  {
    goal: 'lancePromotion',
    sfen: '9/9/9/9/9/9/9/9/L7L b -',
    nbMoves: 4,
    success: obstaclesCaptured,
    obstacles: ['9b', '8a', '1b', '2a'],
  },
  {
    goal: 'lanceSummary',
    sfen: '9/9/9/9/9/4L4/9/9/9 b -',
    nbMoves: 1,
    success: obstaclesCaptured,
    obstacles: ['5d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('L'), '5f')),
  },
  {
    goal: 'planceSummaryTwo',
    sfen: '9/9/9/9/9/4+L4/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('+L'), '5f')),
  },
];

const stage: IncompleteStage = {
  key: 'lance',
  title: 'theLance',
  subtitle: 'itMovesStraightForward',
  intro: 'lanceIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'lanceComplete',
};

export default stage;
