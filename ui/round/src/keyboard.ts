import RoundController from './ctrl';
import { h, VNode } from 'snabbdom';
import * as xhr from 'common/xhr';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';

export const prev = (ctrl: RoundController) => ctrl.userJump(ctrl.ply - 1);

export const next = (ctrl: RoundController) => ctrl.userJump(ctrl.ply + 1);

export const init = (ctrl: RoundController) =>
  window.Mousetrap.bind(['left', 'h'], () => {
    prev(ctrl);
    ctrl.redraw();
  })
    .bind(['right', 'l'], () => {
      next(ctrl);
      ctrl.redraw();
    })
    .bind(['up', 'k'], () => {
      ctrl.userJump(0);
      ctrl.redraw();
    })
    .bind(['down', 'j'], () => {
      ctrl.userJump(ctrl.data.steps.length - 1);
      ctrl.redraw();
    })
    .bind('f', ctrl.flipNow)
    .bind('z', () => lichess.pubsub.emit('zen'))
    .bind('?', () => {
      ctrl.keyboardHelp = !ctrl.keyboardHelp;
      ctrl.redraw();
    });

export const view = (ctrl: RoundController): VNode =>
  snabModal({
    class: 'keyboard-help',
    onInsert: async ($wrap: Cash) => {
      const [, html] = await Promise.all([lichess.loadCssPath('round.keyboard'), xhr.text(xhr.url('/round/help', {}))]);
      $wrap.find('.scrollable').html(html);
    },
    onClose() {
      ctrl.keyboardHelp = false;
      ctrl.redraw();
    },
    content: [h('div.scrollable', spinner())],
  });
