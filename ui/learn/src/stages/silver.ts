import { obstaclesCaptured } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, initial, pieceMovesHighlihts } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: 'grabAllTheStars',
    sfen: '9/9/9/9/9/4S4/9/9/9 b -',
    nbMoves: 4,
    success: obstaclesCaptured,
    obstacles: ['5e', '6d', '8f'],
    drawShapes: initial([arrow('5f', '5e'), arrow('5e', '6d'), arrow('6d', '7e'), arrow('7e', '8f')]),
  },
  {
    goal: 'grabAllTheStars',
    sfen: '9/9/9/4S4/9/9/9/9/9 b -',
    nbMoves: 5,
    success: obstaclesCaptured,
    obstacles: ['4e', '5f', '6g', '5h', '4i'],
  },
  {
    goal: 'theFewerMoves',
    sfen: '9/9/9/9/3S5/6S2/9/9/9 b -',
    nbMoves: 5,
    success: obstaclesCaptured,
    obstacles: ['6f', '6g', '5f', '3e', '2f'],
  },
  {
    goal: 'silverPromotion',
    sfen: '9/9/9/6S2/9/9/9/9/9 b -',
    nbMoves: 3,
    success: obstaclesCaptured,
    obstacles: ['5d', '4d', '4c'],
  },
  {
    goal: 'silverSummary',
    sfen: '9/9/9/9/9/4S4/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('S'), '5f')),
  },
  {
    goal: 'psilverSummary',
    sfen: '9/9/9/9/9/4+S4/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('+S'), '5f')),
  },
];

const stage: IncompleteStage = {
  key: 'silver',
  title: 'theSilver',
  subtitle: 'itMovesEitherForwardOrDiagonallyBack',
  intro: 'silverIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'silverComplete',
};
export default stage;
