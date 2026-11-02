import { h } from 'snabbdom';

import type Ctrl from './ctrl';

export default function (ctrl: Ctrl) {
  return h('div.help.box', [
    h('div.top', 'Definitions'),
    h(
      'div.content',
      (['metric', 'dimension'] as const).map(type => {
        const data = ctrl.vm[type];
        return h('section.' + type, [h('h3', data.name), h('p', data.description)]);
      }),
    ),
  ]);
}
