import RoundController from './ctrl';
import { VNode } from 'snabbdom';
import { snabDialog } from 'common/dialog';

export const prev = (ctrl: RoundController) => ctrl.userJump(ctrl.ply - 1);

export const next = (ctrl: RoundController) => ctrl.userJump(ctrl.ply + 1);

export const init = (ctrl: RoundController) =>
  site.mousetrap
    .bind(['left', 'k'], () => {
      prev(ctrl);
      ctrl.redraw();
    })
    .bind(['right', 'j'], () => {
      next(ctrl);
      ctrl.redraw();
    })
    .bind(['up', '0', 'home'], () => {
      ctrl.userJump(0);
      ctrl.redraw();
    })
    .bind(['down', '$', 'end'], () => {
      ctrl.userJump(ctrl.data.steps.length - 1);
      ctrl.redraw();
    })
    .bind('f', ctrl.flipNow)
    .bind('z', () => site.pubsub.emit('zen'))
    .bind('?', () => {
      ctrl.keyboardHelp = !ctrl.keyboardHelp;
      ctrl.redraw();
    });

export const view = (ctrl: RoundController): VNode =>
  snabDialog({
    class: 'help',
    htmlUrl: '/round/help',
    onClose() {
      ctrl.keyboardHelp = false;
      ctrl.redraw();
    },
  });
