import { h } from 'snabbdom';

import { bind } from 'lib/view';

import { withLinebreaks } from '../util';
import type { RunCtrl } from './runCtrl';

export default function (ctrl: RunCtrl) {
  return h(
    'div.learn__screen-overlay',
    { hook: bind('click', ctrl.hideStartingPane) },
    h('div.learn__screen', [
      h('h1', i18n.learn.stageX(ctrl.stage.id) + ': ' + ctrl.stage.title),
      ctrl.stage.illustration,
      h('p', withLinebreaks(ctrl.stage.intro)),
      h(
        'div.buttons',
        h(
          'button.button',
          {
            key: ctrl.stage.id,
            hook: bind('click', ctrl.hideStartingPane),
          },
          i18n.learn.letsGo,
        ),
      ),
    ]),
  );
}
