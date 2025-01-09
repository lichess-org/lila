import { i18n } from 'i18n';
import { obstaclesCaptured } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, initial, pieceMovesHighlihts } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: i18n('learn:knightsHaveAFancyWay'),
    sfen: '9/9/9/9/9/9/9/9/1N7 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['7g', '8e'],
    drawShapes: initial([arrow('8i', '7g'), arrow('7g', '8e')]),
  },
  {
    goal: i18n('learn:knightPromotion'),
    sfen: '9/9/9/9/9/9/9/1N7/9 b -',
    nbMoves: 5,
    success: obstaclesCaptured,
    obstacles: ['7f', '8d', '7b', '6b', '6c'],
  },
  {
    goal: i18n('learn:knightSummary'),
    sfen: '9/9/9/9/9/4N4/9/9/9 b -',
    nbMoves: 1,
    success: obstaclesCaptured,
    obstacles: ['4d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('N'), '5f')),
  },
  {
    goal: i18n('learn:pknightSummary'),
    sfen: '9/9/9/9/9/4+N4/9/9/9 b -',
    nbMoves: 2,
    success: obstaclesCaptured,
    obstacles: ['5d'],
    squareHighlights: initial(pieceMovesHighlihts(toPiece('+N'), '5f')),
  },
];

const stage: IncompleteStage = {
  key: 'knight',
  title: i18n('learn:theKnight'),
  subtitle: i18n('learn:itMovesInAnLShape'),
  intro: i18n('learn:knightIntro'),
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: i18n('learn:knightComplete'),
};

export default stage;
