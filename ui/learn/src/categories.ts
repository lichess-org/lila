import { i18n } from 'i18n';
import { Category, IncompleteStage, Stage } from './interfaces';
import bishop from './stages/bishop';
import capture from './stages/capture';
import check1 from './stages/check1';
import checkmate1 from './stages/checkmate1';
import drop from './stages/drop';
import gold from './stages/gold';
import intro from './stages/intro';
import king from './stages/king';
import knight from './stages/knight';
import lance from './stages/lance';
import outOfCheck from './stages/out-of-check';
import pawn from './stages/pawn';
import protection from './stages/protection';
import repetition from './stages/repetition';
import rook from './stages/rook';
import setup from './stages/setup';
import silver from './stages/silver';
import value from './stages/value';

function addStageId(): (iStage: IncompleteStage) => Stage {
  let cnt = 0;
  return (iStage: IncompleteStage) => {
    const stage = iStage as Stage;
    stage.id = ++cnt;
    return stage;
  };
}
const createStage = addStageId();

export const categories: Category[] = [
  {
    key: 'theIntroduction',
    name: i18n('learn:theIntroduction'),
    stages: [intro].map(createStage),
  },
  {
    key: 'shogiPieces',
    name: i18n('learn:shogiPieces'),
    stages: [king, gold, silver, knight, lance, bishop, rook, pawn].map(createStage),
  },
  {
    key: 'fundamentals',
    name: i18n('learn:fundamentals'),
    stages: [capture, drop, protection, check1, outOfCheck, checkmate1].map(createStage),
  },
  {
    key: 'intermediate',
    name: i18n('learn:intermediate'),
    stages: [setup, repetition].map(createStage),
  },
  {
    key: 'advanced',
    name: i18n('learn:advanced'),
    stages: [value].map(createStage),
  },
];

export const stages: Stage[] = ([] as Stage[]).concat(...categories.map(c => c.stages));

export function nextStage(stageId: number): Stage | undefined {
  const index = stages.findIndex(s => s.id === stageId);
  return stages[index + 1];
}
