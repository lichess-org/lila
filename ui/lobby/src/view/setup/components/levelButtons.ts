import { h } from 'snabbdom';
import type LobbyController from '@/ctrl';
import { option } from 'lib/setup/option';

const levels = [1, 2, 3, 4, 5, 6, 7, 8];

export const levelButtons = (ctrl: LobbyController) => {
  const { setupCtrl } = ctrl;
  return site.blindMode
    ? [
        h('label', { attrs: { for: 'sf_level' } }, i18n.site.strength),
        h(
          'select#sf_level',
          {
            on: { change: (e: Event) => setupCtrl.aiLevel(parseInt((e.target as HTMLSelectElement).value)) },
          },
          levels
            .map(l => l.toString())
            .map(key => option({ key, name: key }, setupCtrl.aiLevel().toString())),
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
                    checked: level === setupCtrl.aiLevel(),
                  },
                  on: {
                    change: (e: Event) => setupCtrl.aiLevel(parseInt((e.target as HTMLInputElement).value)),
                  },
                }),
                h('label', { attrs: { for: `sf_level_${level}` } }, level),
              ]),
            ),
          ),
        ]),
      ]);
};
