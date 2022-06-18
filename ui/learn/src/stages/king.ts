import { obstaclesCaptured } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, initial, pieceMovesHighlihts } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: 'theKingIsSlow',
    sfen: '9/9/9/9/9/9/3K5/9/9 b -',
    nbMoves: 3,
    success: obstaclesCaptured,
    drawShapes: initial([arrow('6g', '6f'), arrow('6f', '6e'), arrow('6e', '5d')]),
    obstacles: ['5d'],
  },
  {
    goal: 'grabAllTheStars',
    sfen: '9/9/9/5K3/9/9/9/9/9 b -',
    nbMoves: 4,
    success: obstaclesCaptured,
    obstacles: ['6c', '5b', '4c', '4b'],
  },
  {
    goal: 'kingSummary',
    sfen: '9/9/9/9/4K4/9/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5c'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('K'), '5e')),
  },
];

const stage: IncompleteStage = {
  key: 'king',
  title: 'theKing',
  subtitle: 'theMostImportantPiece',
  intro: 'kingIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'kingComplete',
};
export default stage;
