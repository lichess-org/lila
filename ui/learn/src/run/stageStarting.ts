import * as util from '../util';
import { RunCtrl } from './runCtrl';
import { h } from 'snabbdom';
import { bind } from 'common/snabbdom';

export default function (ctrl: RunCtrl) {
  return h(
    'div.learn__screen-overlay',
    { hook: bind('click', ctrl.hideStartingPane) },
    h('div.learn__screen', [
      h('h1', ctrl.trans('stageX', ctrl.stage.id) + ': ' + ctrl.trans.noarg(ctrl.stage.title)),
      ctrl.stage.illustration,
      h('p', util.withLinebreaks(ctrl.trans.noarg(ctrl.stage.intro))),
      h(
        'div.buttons',
        h(
          'a.next',
          {
            key: ctrl.stage.id,
            hook: bind('click', ctrl.hideStartingPane),
          },
          ctrl.trans.noarg('letsGo'),
        ),
      ),
    ]),
  );
}
