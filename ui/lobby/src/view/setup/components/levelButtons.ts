import { h } from 'snabbdom';

import { option } from 'lib/setup/option';
import { radioGroup } from 'lib/view/radio';

import type SetupController from '@/setupCtrl';

const levels = [1, 2, 3, 4, 5, 6, 7, 8].map(String);

export const levelButtons = ({ aiLevel }: SetupController) => {
  return site.blindMode
    ? [
        h('label', { attrs: { for: 'sf_level' } }, i18n.site.strength),
        h(
          'select#sf_level',
          {
            on: { change: (e: Event) => aiLevel(parseInt((e.target as HTMLSelectElement).value)) },
          },
          levels.map(l => l).map(key => option({ key, name: key }, aiLevel().toString())),
        ),
      ]
    : h('div.config-group', [
        h('div.radio-pane', [
          h('div.label', i18n.site.strength),
          radioGroup({
            name: 'sf-level',
            active: aiLevel().toString(),
            onChange: (key: string) => aiLevel(parseInt(key)),
            buttons: levels.map(level => ({ key: level, content: level })),
          }),
        ]),
      ]);
};
