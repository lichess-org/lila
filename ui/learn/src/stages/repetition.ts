import { scenarioFailure, scenarioSuccess } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { concat, custom, onPly } from '../shapes';
import { createScenario } from '../util';

const digits = {
  one: `<svg class="rep-digit" viewBox="24 24 16 16" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="30"/><path d="M38 48h-6.1V25c-2.2 2.1-4.9 3.6-7.9 4.6v-5.5c1.6-.5 3.3-1.5 5.2-3 1.9-1.5 3.2-3.2 3.8-5.1h5v32" fill="#fff"/></svg>`,
  two: `<svg class="rep-digit" viewBox="24 24 16 16" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="30"/><path d="M42 42.3V48H22c.2-2.1.9-4.2 1.9-6.1 1.1-1.9 3.2-4.5 6.4-7.6 2.6-2.6 4.1-4.3 4.7-5.2.8-1.3 1.2-2.5 1.2-3.7 0-1.4-.3-2.4-1-3.1s-1.6-1.1-2.8-1.1c-1.2 0-2.1.4-2.8 1.2-.7.8-1.1 2-1.2 3.8l-5.7-.6c.3-3.4 1.4-5.8 3.2-7.2 1.8-1.5 4-2.2 6.7-2.2 2.9 0 5.2.8 6.9 2.5S42 22.5 42 25c0 1.4-.2 2.8-.7 4.1-.5 1.2-1.3 2.5-2.3 3.9-.7.9-1.9 2.3-3.7 4.1-1.8 1.8-2.9 2.9-3.4 3.5-.5.6-.9 1.1-1.2 1.7H42" fill="#fff"/></svg>`,
  three: `<svg class="rep-digit" viewBox="24 24 16 16" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="30"/><path d="m22 39.2 5.6-.7c.2 1.5.7 2.6 1.4 3.4s1.7 1.2 2.8 1.2c1.2 0 2.2-.5 3-1.4.8-.9 1.2-2.2 1.2-3.8 0-1.5-.4-2.7-1.2-3.6-.8-.9-1.7-1.3-2.9-1.3-.7 0-1.6.1-2.6.4l.6-4.9c1.6 0 2.8-.3 3.6-1.1s1.2-1.7 1.2-3c0-1.1-.3-1.9-.9-2.5-.6-.6-1.4-.9-2.4-.9s-1.8.4-2.5 1.1-1.1 1.8-1.3 3.1l-5.3-.9c.4-1.9.9-3.4 1.7-4.5.7-1.1 1.8-2 3.1-2.7 1.3-.6 2.8-1 4.5-1 2.8 0 5.1.9 6.8 2.8 1.4 1.5 2.1 3.3 2.1 5.2 0 2.8-1.4 4.9-4.3 6.6 1.7.4 3.1 1.2 4.1 2.6 1 1.3 1.5 3 1.5 4.9 0 2.8-1 5.1-2.9 7-1.7 1.8-4.1 2.8-7 2.8-2.7 0-5-.8-6.8-2.4-1.8-1.7-2.8-3.8-3.1-6.4" fill="#fff"/></svg>`,
  four: `<svg class="rep-digit" viewBox="24 24 16 16" xmlns="http://www.w3.org/2000/svg"><circle cx="32" cy="32" r="30"/><path d="M33.7 48v-6.4H20v-5.3L34.5 16h5.4v20.2H44v5.4h-4.1V48h-6.2zm0-11.8V25.3L26 36.2h7.7z" fill="#fff"/></svg>`,
};

const levels: IncompleteLevel[] = [
  {
    goal: 'ifTheSamePositionOccurs',
    sfen: '9/9/9/9/k8/9/+p8/3+p5/K8 b - 1',
    nbMoves: 12,
    success: scenarioSuccess,
    failure: scenarioFailure,
    drawShapes: concat(
      onPly(0, [custom('5e', digits.one)]),
      onPly(4, [custom('5e', digits.two)]),
      onPly(8, [custom('5e', digits.three)]),
      onPly(12, [custom('5e', digits.four)])
    ),
    scenario: createScenario(
      ['9i8i', '9e8e', '8i9i', '8e9e', '9i8i', '9e8e', '8i9i', '8e9e', '9i8i', '9e8e', '8i9i', '8e9e'],
      'sente',
      true
    ),
  },
  {
    goal: 'perpetualCheckIsALoss',
    sfen: '3k5/9/1s+P+P+P4/9/P2+b5/KP7/1+b7/9/9 b -',
    nbMoves: 12,
    success: scenarioSuccess,
    failure: scenarioFailure,
    scenario: createScenario(
      ['9f8e', '8g7f', '8e9f', '7f8g', '9f8e', '8g7f', '8e9f', '7f8g', '9f8e', '8g7f', '8e9f', '7f8g'],
      'sente',
      true
    ),
  },
  {
    goal: 'boardFlippedFindBestMove',
    sfen: '3k5/9/1s+P+P+P4/9/P2+b5/KP+b6/9/9/9 w -',
    nbMoves: 3,
    success: scenarioSuccess,
    failure: scenarioFailure,
    scenario: createScenario(['7f8g', '9f8e', '6e7f'], 'gote', true),
    showFailureMove: 'random',
  },
];

const stage: IncompleteStage = {
  key: 'repetition',
  title: 'repetition',
  subtitle: 'fourfoldRepetitionIsADrawExcept',
  intro: 'repetitionIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'repetitionComplete',
};

export default stage;
