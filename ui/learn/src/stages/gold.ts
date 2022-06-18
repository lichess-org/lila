import { obstaclesCaptured } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, initial, pieceMovesHighlihts } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: 'grabAllTheStars',
    sfen: '9/9/9/9/9/4G4/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['4d'],
    drawShapes: initial([arrow('5f', '5e'), arrow('5e', '4d')]),
  },
  {
    goal: 'theFewerMoves',
    sfen: '9/9/9/9/1G7/9/9/9/9 b -',
    nbMoves: 4,
    success: obstaclesCaptured,
    obstacles: ['8g', '8f', '7g', '6f'],
  },
  {
    goal: 'theFewerMoves',
    sfen: '9/9/9/9/9/9/9/9/3G5 b -',
    nbMoves: 8,
    success: obstaclesCaptured,
    obstacles: ['6h', '7g', '8f', '8e', '7d', '6c', '6d', '5d'],
  },
  {
    goal: 'goldDoesntPromote',
    sfen: '9/9/9/6G2/9/9/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['2c', '3b'],
  },
  {
    goal: 'goldSummary',
    sfen: '9/9/9/9/9/4G4/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('G'), '5f')),
  },
];

const stage: IncompleteStage = {
  key: 'gold',
  title: 'theGold',
  subtitle: 'itMovesInAnyDirectionExceptDiagonallyBack',
  intro: 'goldIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'goldComplete',
};
export default stage;
