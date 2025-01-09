import { i18n } from 'i18n';
import { obstaclesCaptured } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, initial, pieceMovesHighlihts } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: i18n('learn:grabAllTheStars'),
    sfen: '9/9/9/9/9/4S4/9/9/9 b -',
    nbMoves: 4,
    success: obstaclesCaptured,
    obstacles: ['5e', '6d', '8f'],
    drawShapes: initial([arrow('5f', '5e'), arrow('5e', '6d'), arrow('6d', '7e'), arrow('7e', '8f')]),
  },
  {
    goal: i18n('learn:grabAllTheStars'),
    sfen: '9/9/9/4S4/9/9/9/9/9 b -',
    nbMoves: 5,
    success: obstaclesCaptured,
    obstacles: ['4e', '5f', '6g', '5h', '4i'],
  },
  {
    goal: i18n('learn:theFewerMoves'),
    sfen: '9/9/9/9/3S5/6S2/9/9/9 b -',
    nbMoves: 5,
    success: obstaclesCaptured,
    obstacles: ['6f', '6g', '5f', '3e', '2f'],
  },
  {
    goal: i18n('learn:silverPromotion'),
    sfen: '9/9/9/6S2/9/9/9/9/9 b -',
    nbMoves: 3,
    success: obstaclesCaptured,
    obstacles: ['5d', '4d', '4c'],
  },
  {
    goal: i18n('learn:silverSummary'),
    sfen: '9/9/9/9/9/4S4/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('S'), '5f')),
  },
  {
    goal: i18n('learn:psilverSummary'),
    sfen: '9/9/9/9/9/4+S4/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('+S'), '5f')),
  },
];

const stage: IncompleteStage = {
  key: 'silver',
  title: i18n('learn:theSilver'),
  subtitle: i18n('learn:itMovesEitherForwardOrDiagonallyBack'),
  intro: i18n('learn:silverIntro'),
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: i18n('learn:silverComplete'),
};
export default stage;
