import { obstaclesCaptured } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, initial, pieceMovesHighlihts } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: 'knightsHaveAFancyWay',
    sfen: '9/9/9/9/9/9/9/9/1N7 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['7g', '8e'],
    drawShapes: initial([arrow('8i', '7g'), arrow('7g', '8e')]),
  },
  {
    goal: 'knightPromotion',
    sfen: '9/9/9/9/9/9/9/1N7/9 b -',
    nbMoves: 5,
    success: obstaclesCaptured,
    obstacles: ['7f', '8d', '7b', '6b', '6c'],
  },
  {
    goal: 'knightSummary',
    sfen: '9/9/9/9/9/4N4/9/9/9 b -',
    nbMoves: 1,
    success: obstaclesCaptured,
    obstacles: ['4d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('N'), '5f')),
  },
  {
    goal: 'pknightSummary',
    sfen: '9/9/9/9/9/4+N4/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('+N'), '5f')),
  },
];

const stage: IncompleteStage = {
  key: 'knight',
  title: 'theKnight',
  subtitle: 'itMovesInAnLShape',
  intro: 'knightIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'knightComplete',
};

export default stage;
