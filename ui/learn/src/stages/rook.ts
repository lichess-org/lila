import { i18n } from 'i18n';
import { obstaclesCaptured } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, initial, pieceMovesHighlihts } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: i18n('learn:rookGoal'),
    sfen: '9/9/9/9/9/9/9/4R4/9 b -',
    nbMoves: 1,
    success: obstaclesCaptured,
    obstacles: ['5d'],
    drawShapes: initial([arrow('5h', '5d')]),
  },
  {
    goal: i18n('learn:grabAllTheStarsRemember'),
    sfen: '9/9/9/9/9/9/9/9/9 b R',
    nbMoves: 3,
    success: obstaclesCaptured,
    obstacles: ['7g', '3g'],
  },
  {
    goal: i18n('learn:rookPromotion'),
    sfen: '9/7R1/9/9/9/9/9/9/9 b -',
    nbMoves: 4,
    success: obstaclesCaptured,
    obstacles: ['2h', '3i', '3b', '4c'],
  },
  {
    goal: i18n('learn:rookSummary'),
    sfen: '9/9/9/9/4R4/9/9/9/9 b -',
    nbMoves: 1,
    success: obstaclesCaptured,
    obstacles: ['5i'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('R'), '5e')),
  },
  {
    goal: i18n('learn:dragonSummaryTwo'),
    sfen: '9/9/9/9/4+R4/9/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5b', '6c'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('+R'), '5e')),
  },
];

const stage: IncompleteStage = {
  key: 'rook',
  title: i18n('learn:theRook'),
  subtitle: i18n('learn:itMovesInStraightLines'),
  intro: i18n('learn:rookIntro'),
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: i18n('learn:rookComplete'),
};
export default stage;
