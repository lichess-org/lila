import m from './mithrilFix';
import { LevelCtrl } from './level';
import * as scoring from './score';
import { Level, Stage } from './stage/list';
import { LearnProgress } from './main';

const star = m('i[data-icon=]');

export function makeStars(level: Level, score: number) {
  const rank = scoring.getLevelRank(level, score);
  const stars = [];
  for (let i = 3; i >= rank; i--) stars.push(star);
  return m('span.stars.st' + stars.length, stars);
}

export interface Progress {
  stage: Stage;
  level: LevelCtrl;
  score(level: Level): number;
}

export function ctrl(stage: Stage, level: LevelCtrl, data: LearnProgress): Progress {
  return {
    stage: stage,
    level: level,
    score: function (level: Level) {
      return data.stages[stage.key] ? data.stages[stage.key].scores[level.id - 1] : 0;
    },
  };
}

export function view(ctrl: Progress) {
  return m(
    'div.progress',
    ctrl.stage.levels.map(function (level: Level) {
      const score = ctrl.score(level);
      const status = level.id === ctrl.level.blueprint.id ? 'active' : score ? 'done' : 'future';
      const label = score ? makeStars(level, score) : m('span.id', level.id);
      return m(
        'a',
        {
          href: '/' + ctrl.stage.id + '/' + level.id,
          config: m.route,
          class: status,
        },
        label
      );
    })
  );
}
