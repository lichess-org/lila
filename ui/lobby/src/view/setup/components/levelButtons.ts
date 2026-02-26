import { h } from 'snabbdom';
import { option } from 'lib/setup/option';
import type SetupController from '@/setupCtrl';

const levels = [1, 2, 3, 4, 5, 6, 7, 8];

export const levelButtons = ({ aiLevel }: SetupController) => {
  return site.blindMode
    ? [
        h('label', { attrs: { for: 'sf_level' } }, i18n.site.strength),
        h(
          'select#sf_level',
          {
            on: { change: (e: Event) => aiLevel(parseInt((e.target as HTMLSelectElement).value)) },
          },
          levels.map(l => l.toString()).map(key => option({ key, name: key }, aiLevel().toString())),
        ),
      ]
    : h('div.config-group', [
        h('div.radio-pane', [
          h('div.label', i18n.site.strength),
          h(
            'group.radio',
            levels.map(level =>
              h('div', [
                h(`input#sf_level_${level}`, {
                  attrs: {
                    name: 'level',
                    type: 'radio',
                    value: level,
                    checked: level === aiLevel(),
                  },
                  on: {
                    change: (e: Event) => aiLevel(parseInt((e.target as HTMLInputElement).value)),
                  },
                }),
                h('label', { attrs: { for: `sf_level_${level}` } }, level),
              ]),
            ),
          ),
        ]),
      ]);
};
