import { h } from 'snabbdom';

import { option, aiLevels } from 'lib/setup/option';

import type SetupController from '@/setupCtrl';

export const levelButtons = ({ aiLevel }: SetupController) => {
  return site.blindMode
    ? [
        h('label', { attrs: { for: 'sf_level' } }, i18n.site.strength),
        h(
          'select#sf_level',
          {
            on: { change: (e: Event) => aiLevel(parseInt((e.target as HTMLSelectElement).value)) },
          },
          aiLevels.map(l => l.toString()).map(key => option({ key, name: key }, aiLevel().toString())),
        ),
      ]
    : h('div.config-group', [
        h('div.radio-pane', [
          h('div.label', i18n.site.strength),
          h(
            'group.radio',
            aiLevels.map(level =>
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
