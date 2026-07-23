import { licon } from 'lib/licon';
import { a, div, iconTag, span } from 'lib/view';

import { hashHref } from './hashRouting';
import type { RunCtrl } from './run/runCtrl';
import { getLevelRank } from './score';
import type { Level } from './stage/list';

export function makeStars(level: Level, score: number) {
  const rank = getLevelRank(level, score);
  const stars = [];
  for (let i = 3; i >= rank; i--) stars.push(iconTag(licon.Star));
  return span(`.stars.st${stars.length}`, stars);
}

export function progressView(ctrl: RunCtrl) {
  return div(
    '.progress',
    ctrl.stage.levels.map(function (level: Level) {
      const score = ctrl.score(level);
      const status = level.id === ctrl.levelCtrl.blueprint.id ? 'active' : score ? 'done' : 'future';
      const label = score ? makeStars(level, score) : span('.id', level.id);
      return a(hashHref(ctrl.stage.id, level.id))(`.${status}`, label);
    }),
  );
}
