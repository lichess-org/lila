import * as util from '../util';
import { RunCtrl } from './runCtrl';
import { h } from 'snabbdom';

export default function (ctrl: RunCtrl) {
  return h(
    'div.learn__screen-overlay',
    {
      onclick: ctrl.hideStartingPane,
    },
    h('div.learn__screen', [
      h('h1', ctrl.trans('stageX', ctrl.stage.id) + ': ' + ctrl.trans.noarg(ctrl.stage.title)),
      ctrl.stage.illustration,
      h('p', util.withLinebreaks(ctrl.trans.noarg(ctrl.stage.intro))),
      h(
        'div.buttons',
        h(
          'a.next',
          {
            onclick: ctrl.hideStartingPane,
          },
          ctrl.trans.noarg('letsGo'),
        ),
      ),
    ]),
  );
}
