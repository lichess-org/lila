import { h } from 'snabbdom';
import LearnCtrl from '../../ctrl';

export default function (ctrl: LearnCtrl) {
  if (!ctrl.vm) return null;
  else
    return h(
      'div.learn__screen-overlay.starting',
      {
        on: {
          click: () => {
            if (ctrl.vm) ctrl.vm.stageState = 'running';
            ctrl.redraw();
          },
        },
      },
      h('div.learn__screen.', [
        h('h1', ctrl.trans('stageX', ctrl.vm.stage.id) + ': ' + ctrl.trans.noarg(ctrl.vm.stage.title)),
        h('div.stage-img.' + ctrl.vm.stage.key),
        h('p', ctrl.trans.noarg(ctrl.vm.stage.intro)),
        h('div.buttons', h('a.next', ctrl.trans.noarg('letsGo'))),
      ])
    );
}
