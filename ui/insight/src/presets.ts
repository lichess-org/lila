import { bind } from 'common/snabbdom';
import { h } from 'snabbdom';
import Ctrl from './ctrl';

export default function (ctrl: Ctrl) {
  return h(
    'div.box.presets',
    ctrl.ui.presets.map(function (p) {
      const active = ctrl.makeUrl(p.dimension, p.metric, p.filters) === ctrl.makeCurrentUrl();
      return h(
        'a.preset.text',
        {
          class: { active },
          attrs: { 'data-icon': '' },
          hook: bind('click', () => ctrl.setQuestion(p)),
        },
        p.name
      );
    })
  );
}
