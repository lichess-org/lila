import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import config from '../config';
import { Run } from '../interfaces';
import { getNow } from '../util';

export const playModifiers = (run: Run) => {
  const now = getNow();
  const malus = run.modifier.malus;
  const bonus = run.modifier.bonus;
  return {
    'storm--mod-puzzle': run.current.startAt > now - 90,
    'storm--mod-move': run.modifier.moveAt > now - 90,
    'storm--mod-malus-slow': !!malus && malus.at > now - 950,
    'storm--mod-bonus-slow': !!bonus && bonus.at > now - 950,
  };
};

export const renderCombo = (run: Run): VNode => {
  const level = run.combo.level();
  return h('div.puz-combo', [
    h('div.puz-combo__counter', [
      h('span.puz-combo__counter__value', run.combo.current),
      h('span.puz-combo__counter__combo', 'COMBO'),
    ]),
    h('div.puz-combo__bars', [
      h('div.puz-combo__bar', [
        h('div.puz-combo__bar__in', {
          attrs: { style: `width:${run.combo.percent()}%` },
        }),
        h('div.puz-combo__bar__in-full'),
      ]),
      h(
        'div.puz-combo__levels',
        [0, 1, 2, 3].map(l =>
          h(
            'div.puz-combo__level',
            {
              class: {
                active: l < level,
              },
            },
            h('span', `${config.combo.levels[l + 1][1]}s`)
          )
        )
      ),
    ]),
  ]);
};
