import { h } from 'snabbdom';
import Ctrl from './ctrl';

export default function (ctrl: Ctrl) {
  return h('div.help.box', [
    h('div.top', 'Definitions'),
    h(
      'div.content',
      ['metric', 'dimension'].map(type => {
        const data = ctrl.vm[type as 'metric' | 'dimension'];
        return h('section.' + type, [h('h3', data.name), h('p', data.description)]);
      })
    ),
  ]);
}
