import { i18n } from 'i18n';
import type { SquareHighlight } from 'shogiground/draw';
import type { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, initial } from '../shapes';

const levels: IncompleteLevel[] = [
  {
    goal: i18n('learn:choosePieceDesign'),
    sfen: '9/9/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b -',
    nbMoves: 1,
    success: () => true,
    text: i18n('learn:clickHereAfterYouveChosen'),
  },
  {
    goal: i18n('learn:promotionZone'),
    sfen: '9/9/9/4P4/9/9/9/9/9 b -',
    nbMoves: 1,
    success: () => true,
    drawShapes: initial([arrow('5d', '5c')]),
    squareHighlights: () =>
      [
        '9a',
        '8a',
        '7a',
        '6a',
        '5a',
        '4a',
        '3a',
        '2a',
        '1a',
        '9b',
        '8b',
        '7b',
        '6b',
        '5b',
        '4b',
        '3b',
        '2b',
        '1b',
        '9c',
        '8c',
        '7c',
        '6c',
        '5c',
        '4c',
        '3c',
        '2c',
        '1c',
      ].map(sq => {
        return { key: sq, className: 'help' } as SquareHighlight;
      }),
  },
  {
    goal: i18n('learn:senteGoesFirst'),
    sfen: '9/9/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b -',
    nbMoves: 1,
    success: () => true,
  },
];

const stage: IncompleteStage = {
  key: 'introduction',
  title: i18n('learn:theIntroduction'),
  subtitle: i18n('learn:introBasics'),
  intro: i18n('learn:introIntro'),
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: i18n('learn:introCompleteTwo'),
};
export default stage;
