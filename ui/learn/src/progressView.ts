import * as scoring from './score';
import { Level } from './stage/list';
import { h } from 'snabbdom';
import { hashHref } from './hashRouting';
import { RunCtrl } from './run/runCtrl';
import * as licon from 'common/licon';

const star = h('i', { attrs: { 'data-icon': licon.Star } });

export function makeStars(level: Level, score: number) {
  const rank = scoring.getLevelRank(level, score);
  const stars = [];
  for (let i = 3; i >= rank; i--) stars.push(star);
  return h('span.stars.st' + stars.length, stars);
}

export function progressView(ctrl: RunCtrl) {
  return h(
    'div.progress',
    ctrl.stage.levels.map(function (level: Level) {
      const score = ctrl.score(level);
      const status = level.id === ctrl.levelCtrl.blueprint.id ? 'active' : score ? 'done' : 'future';
      const label = score ? makeStars(level, score) : h('span.id', level.id);
      return h(`a.${status}`, { attrs: { href: hashHref(ctrl.stage.id, level.id) } }, label);
    }),
  );
}
