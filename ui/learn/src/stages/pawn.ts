import { obstaclesCaptured } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, initial, pieceMovesHighlihts } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: 'pawnsMoveOneSquareOnly',
    sfen: '9/9/9/9/9/9/2P6/9/9 b -',
    nbMoves: 3,
    success: obstaclesCaptured,
    obstacles: ['7d'],
    drawShapes: initial([arrow('7g', '7f'), arrow('7f', '7e'), arrow('7e', '7d')]),
  },
  {
    goal: 'pawnPromotion',
    sfen: '9/9/9/9/9/9/6P2/9/9 b -',
    nbMoves: 5,
    success: obstaclesCaptured,
    obstacles: ['2c'],
  },
  {
    goal: 'pawnSummary',
    sfen: '9/9/9/9/9/4P4/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('P'), '5f')),
  },
  {
    goal: 'tokinSummary',
    sfen: '9/9/9/9/9/4+P4/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('+P'), '5f')),
  },
];

const stage: IncompleteStage = {
  key: 'pawn',
  title: 'thePawn',
  subtitle: 'itMovesForwardOnly',
  intro: 'pawnIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'pawnComplete',
};

export default stage;
